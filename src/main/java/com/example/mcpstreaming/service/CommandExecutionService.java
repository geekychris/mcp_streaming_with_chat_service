package com.example.mcpstreaming.service;

import com.example.mcpstreaming.model.CommandResult.CommandExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for executing system commands with reactive streaming support.
 * Provides both blocking and streaming command execution with security considerations.
 */
@Service
public class CommandExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutionService.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final List<String> DANGEROUS_COMMANDS = Arrays.asList(
        "rm", "rmdir", "del", "format", "fdisk", "mkfs", "dd", "shutdown", "reboot", "halt"
    );
    
    /**
     * Executes a command and returns the complete result.
     */
    public Mono<CommandExecution> executeCommand(String command, String workingDirectory, Duration timeout) {
        return Mono.fromCallable(() -> {
            validateCommand(command);
            
            long startTime = System.currentTimeMillis();
            
            ProcessBuilder processBuilder = createProcessBuilder(command, workingDirectory);
            
            try {
                Process process = processBuilder.start();
                
                // Read stdout and stderr
                StringBuilder stdout = new StringBuilder();
                StringBuilder stderr = new StringBuilder();
                
                // Read streams in parallel
                Thread stdoutReader = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stdout.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        logger.warn("Error reading stdout: {}", e.getMessage());
                    }
                });
                
                Thread stderrReader = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stderr.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        logger.warn("Error reading stderr: {}", e.getMessage());
                    }
                });
                
                stdoutReader.start();
                stderrReader.start();
                
                // Wait for process completion with timeout
                boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
                
                if (!finished) {
                    process.destroyForcibly();
                    throw new RuntimeException("Command timed out after " + timeout.toMillis() + "ms");
                }
                
                // Wait for stream readers to complete
                stdoutReader.join(1000);
                stderrReader.join(1000);
                
                long executionTime = System.currentTimeMillis() - startTime;
                int exitCode = process.exitValue();
                
                return new CommandExecution(
                    command,
                    exitCode,
                    stdout.toString().trim(),
                    stderr.toString().trim(),
                    executionTime
                );
                
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error executing command: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Executes a command with default timeout.
     */
    public Mono<CommandExecution> executeCommand(String command, String workingDirectory) {
        return executeCommand(command, workingDirectory, DEFAULT_TIMEOUT);
    }
    
    /**
     * Executes a command and streams the output in real-time.
     */
    public Flux<String> executeCommandStream(String command, String workingDirectory, boolean includeStderr) {
        return Flux.create(sink -> {
            try {
                validateCommand(command);
                
                ProcessBuilder processBuilder = createProcessBuilder(command, workingDirectory);
                Process process = processBuilder.start();
                
                // Stream stdout
                Thread stdoutReader = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sink.next("STDOUT: " + line);
                        }
                    } catch (IOException e) {
                        sink.error(new RuntimeException("Error reading stdout: " + e.getMessage(), e));
                    }
                });
                
                final Thread stderrReaderRef;
                if (includeStderr) {
                    stderrReaderRef = new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sink.next("STDERR: " + line);
                            }
                        } catch (IOException e) {
                            sink.error(new RuntimeException("Error reading stderr: " + e.getMessage(), e));
                        }
                    });
                } else {
                    stderrReaderRef = null;
                }
                
                stdoutReader.start();
                if (stderrReaderRef != null) {
                    stderrReaderRef.start();
                }
                
                // Wait for process completion
                new Thread(() -> {
                    try {
                        int exitCode = process.waitFor();
                        stdoutReader.join();
                        if (stderrReaderRef != null) {
                            stderrReaderRef.join();
                        }
                        sink.next("EXIT_CODE: " + exitCode);
                        sink.complete();
                    } catch (InterruptedException e) {
                        process.destroyForcibly();
                        sink.error(new RuntimeException("Command execution interrupted", e));
                    }
                }).start();
                
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
    
    /**
     * Creates a ProcessBuilder for the given command and working directory.
     */
    private ProcessBuilder createProcessBuilder(String command, String workingDirectory) {
        ProcessBuilder processBuilder;
        
        // Determine if we're on Windows or Unix-like system
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows
            processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            // Unix-like systems (Linux, macOS)
            processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
        }
        
        // Set working directory if specified
        if (workingDirectory != null && !workingDirectory.trim().isEmpty()) {
            processBuilder.directory(new java.io.File(workingDirectory));
        }
        
        // Merge stderr into stdout for simpler handling in some cases
        // processBuilder.redirectErrorStream(true);
        
        return processBuilder;
    }
    
    /**
     * Validates commands for basic security.
     * This is a simple whitelist/blacklist approach - in production, you'd want more sophisticated security.
     */
    private void validateCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new RuntimeException("Command cannot be null or empty");
        }
        
        String lowerCommand = command.toLowerCase().trim();
        
        // Check for dangerous commands
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (lowerCommand.startsWith(dangerous + " ") || lowerCommand.equals(dangerous)) {
                throw new RuntimeException("Dangerous command not allowed: " + dangerous);
            }
        }
        
        // Additional security checks
        if (lowerCommand.contains("sudo") || lowerCommand.contains("su ")) {
            throw new RuntimeException("Privilege escalation commands not allowed");
        }
        
        if (lowerCommand.contains(">/dev/") || lowerCommand.contains(">/proc/")) {
            throw new RuntimeException("Writing to system devices not allowed");
        }
        
        logger.info("Executing command: {}", command);
    }
    
    /**
     * Gets information about the current system.
     */
    public Mono<CommandExecution> getSystemInfo() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("win")) {
            return executeCommand("systeminfo", null);
        } else {
            return executeCommand("uname -a && uptime && whoami", null);
        }
    }
    
    /**
     * Lists running processes.
     */
    public Mono<CommandExecution> listProcesses() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("win")) {
            return executeCommand("tasklist", null);
        } else {
            return executeCommand("ps aux", null);
        }
    }
}