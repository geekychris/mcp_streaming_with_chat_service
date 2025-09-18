package com.example.mcpstreaming.controller;

import com.example.mcpstreaming.model.*;
import com.example.mcpstreaming.service.CommandExecutionService;
import com.example.mcpstreaming.service.FileOperationService;
import com.example.mcpstreaming.service.GrepService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST controller for handling MCP streaming requests and responses.
 * Implements the MCP protocol over HTTP with support for both single responses and streaming.
 */
@RestController
@RequestMapping("/api/mcp")
public class McpStreamingController {
    
    private static final Logger logger = LoggerFactory.getLogger(McpStreamingController.class);
    
    @Autowired
    private FileOperationService fileOperationService;
    
    @Autowired
    private GrepService grepService;
    
    @Autowired
    private CommandExecutionService commandExecutionService;
    
    /**
     * Handles MCP requests and returns appropriate responses.
     */
    @PostMapping(value = "/request", 
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<McpMessage> handleRequest(@RequestBody McpRequest request) {
        logger.info("Received MCP request: operation={}, stream={}", request.getOperation(), request.isStream());
        
        try {
            if (request.isStream()) {
                // For streaming requests, return an initial response indicating streaming will begin
                McpResponse response = new McpResponse(request.getId(), "streaming", "Stream initiated");
                response.setStreamComplete(false);
                return Mono.just(response);
            } else {
                // Handle non-streaming request
                return handleNonStreamingRequest(request);
            }
        } catch (Exception e) {
            logger.error("Error handling MCP request", e);
            return Mono.just(new McpError(
                request.getId(),
                "REQUEST_ERROR",
                "Error processing request: " + e.getMessage(),
                e.getClass().getSimpleName()
            ));
        }
    }
    
    /**
     * Handles streaming MCP requests and returns a stream of chunks.
     */
    @PostMapping(value = "/stream", 
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<McpMessage> handleStreamingRequest(@RequestBody McpRequest request) {
        logger.info("Received MCP streaming request: operation={}", request.getOperation());
        
        try {
            return handleStreamingOperation(request);
        } catch (Exception e) {
            logger.error("Error handling streaming MCP request", e);
            return Flux.just(new McpError(
                request.getId(),
                "STREAM_ERROR",
                "Error processing streaming request: " + e.getMessage(),
                e.getClass().getSimpleName()
            ));
        }
    }
    
    /**
     * Lists available operations.
     */
    @GetMapping(value = "/operations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> listOperations() {
        return Mono.just(Map.of(
            "operations", Map.of(
                "list_directory", Map.of(
                    "description", "Lists files and directories in a path",
                    "parameters", Map.of(
                        "path", "string - directory path to list"
                    ),
                    "streaming", true
                ),
                "read_file", Map.of(
                    "description", "Reads content of a file",
                    "parameters", Map.of(
                        "path", "string - file path to read"
                    ),
                    "streaming", true
                ),
                "create_file", Map.of(
                    "description", "Creates a new file with content",
                    "parameters", Map.of(
                        "path", "string - file path to create",
                        "content", "string - file content"
                    ),
                    "streaming", false
                ),
                "edit_file", Map.of(
                    "description", "Edits (overwrites) a file with new content",
                    "parameters", Map.of(
                        "path", "string - file path to edit",
                        "content", "string - new file content"
                    ),
                    "streaming", false
                ),
                "append_file", Map.of(
                    "description", "Appends content to an existing file",
                    "parameters", Map.of(
                        "path", "string - file path to append to",
                        "content", "string - content to append"
                    ),
                    "streaming", false
                ),
                "grep", Map.of(
                    "description", "Searches for patterns in files",
                    "parameters", Map.of(
                        "pattern", "string - regex pattern to search for",
                        "path", "string - file or directory path to search",
                        "recursive", "boolean - search recursively (default: false)",
                        "case_sensitive", "boolean - case sensitive search (default: true)"
                    ),
                    "streaming", true
                ),
                "execute_command", Map.of(
                    "description", "Executes a system command",
                    "parameters", Map.of(
                        "command", "string - command to execute",
                        "working_directory", "string - working directory (optional)",
                        "timeout_seconds", "integer - timeout in seconds (optional)"
                    ),
                    "streaming", true
                )
            )
        ));
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of(
            "status", "UP",
            "service", "MCP Streaming Service",
            "version", "1.0.0"
        ));
    }
    
    /**
     * Handles non-streaming MCP requests.
     */
    private Mono<McpMessage> handleNonStreamingRequest(McpRequest request) {
        return switch (request.getOperation()) {
            case "list_directory" -> {
                String path = getStringParameter(request, "path", ".");
                yield fileOperationService.listDirectory(path)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "read_file" -> {
                String path = getStringParameter(request, "path");
                yield fileOperationService.readFile(path)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "create_file" -> {
                String path = getStringParameter(request, "path");
                String content = getStringParameter(request, "content", "");
                yield fileOperationService.createFile(path, content)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "edit_file" -> {
                String path = getStringParameter(request, "path");
                String content = getStringParameter(request, "content");
                yield fileOperationService.editFile(path, content)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "append_file" -> {
                String path = getStringParameter(request, "path");
                String content = getStringParameter(request, "content");
                yield fileOperationService.appendToFile(path, content)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "grep" -> {
                String pattern = getStringParameter(request, "pattern");
                String path = getStringParameter(request, "path", ".");
                boolean recursive = getBooleanParameter(request, "recursive", false);
                boolean caseSensitive = getBooleanParameter(request, "case_sensitive", true);
                yield grepService.grep(pattern, path, recursive, caseSensitive)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "execute_command" -> {
                String command = getStringParameter(request, "command");
                String workingDir = getStringParameter(request, "working_directory", null);
                int timeoutSeconds = getIntParameter(request, "timeout_seconds", 300);
                Duration timeout = Duration.ofSeconds(timeoutSeconds);
                yield commandExecutionService.executeCommand(command, workingDir, timeout)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            default -> Mono.just(new McpError(
                request.getId(),
                "UNKNOWN_OPERATION",
                "Unknown operation: " + request.getOperation()
            ));
        };
    }
    
    /**
     * Handles streaming MCP requests.
     */
    private Flux<McpMessage> handleStreamingOperation(McpRequest request) {
        AtomicLong sequenceNumber = new AtomicLong(0);
        
        return switch (request.getOperation()) {
            case "list_directory" -> {
                String path = getStringParameter(request, "path", ".");
                yield fileOperationService.listDirectoryStream(path)
                    .map(fileInfo -> new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        fileInfo
                    ))
                    .cast(McpMessage.class)
                    .concatWith(Flux.just(new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        "STREAM_COMPLETE",
                        true
                    )));
            }
            case "read_file" -> {
                String path = getStringParameter(request, "path");
                yield fileOperationService.readFileStream(path)
                    .map(chunk -> new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        chunk
                    ))
                    .cast(McpMessage.class)
                    .concatWith(Flux.just(new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        "STREAM_COMPLETE",
                        true
                    )));
            }
            case "grep" -> {
                String pattern = getStringParameter(request, "pattern");
                String path = getStringParameter(request, "path", ".");
                boolean recursive = getBooleanParameter(request, "recursive", false);
                boolean caseSensitive = getBooleanParameter(request, "case_sensitive", true);
                yield grepService.grepStream(pattern, path, recursive, caseSensitive)
                    .map(match -> new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        match
                    ))
                    .cast(McpMessage.class)
                    .concatWith(Flux.just(new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        "STREAM_COMPLETE",
                        true
                    )));
            }
            case "execute_command" -> {
                String command = getStringParameter(request, "command");
                String workingDir = getStringParameter(request, "working_directory", null);
                boolean includeStderr = getBooleanParameter(request, "include_stderr", true);
                yield commandExecutionService.executeCommandStream(command, workingDir, includeStderr)
                    .map(output -> new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        output
                    ))
                    .cast(McpMessage.class)
                    .concatWith(Flux.just(new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        "STREAM_COMPLETE",
                        true
                    )));
            }
            default -> Flux.just(new McpError(
                request.getId(),
                "UNKNOWN_OPERATION",
                "Unknown streaming operation: " + request.getOperation()
            ));
        };
    }
    
    // Utility methods for parameter extraction
    private String getStringParameter(McpRequest request, String name) {
        Map<String, Object> params = request.getParameters();
        if (params == null || !params.containsKey(name)) {
            throw new RuntimeException("Missing required parameter: " + name);
        }
        return params.get(name).toString();
    }
    
    private String getStringParameter(McpRequest request, String name, String defaultValue) {
        Map<String, Object> params = request.getParameters();
        if (params == null || !params.containsKey(name)) {
            return defaultValue;  // Return the default value (which could be null)
        }
        return params.get(name).toString();
    }
    
    private boolean getBooleanParameter(McpRequest request, String name, boolean defaultValue) {
        Map<String, Object> params = request.getParameters();
        if (params == null || !params.containsKey(name)) {
            return defaultValue;
        }
        Object value = params.get(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    private int getIntParameter(McpRequest request, String name, int defaultValue) {
        Map<String, Object> params = request.getParameters();
        if (params == null || !params.containsKey(name)) {
            return defaultValue;
        }
        Object value = params.get(name);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}