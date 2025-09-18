package com.example.mcpstreaming.client;

import com.example.mcpstreaming.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Command-line test client for the MCP Streaming Service.
 * Demonstrates all available MCP operations and streaming capabilities.
 */
public class McpTestClient {
    
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api/mcp";
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    private final String baseUrl;
    private final HttpClient httpClient;
    
    public McpTestClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    public static void main(String[] args) {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        
        try {
            CommandLine cmd = parser.parse(options, args);
            
            String baseUrl = cmd.getOptionValue("url", DEFAULT_BASE_URL);
            McpTestClient client = new McpTestClient(baseUrl);
            
            if (cmd.hasOption("interactive")) {
                client.runInteractiveMode();
            } else if (cmd.hasOption("demo")) {
                client.runDemo();
            } else {
                client.runSingleCommand(cmd);
            }
            
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            printHelp(options);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Options createOptions() {
        Options options = new Options();
        
        options.addOption("h", "help", false, "Show help");
        options.addOption("u", "url", true, "Base URL of MCP service (default: " + DEFAULT_BASE_URL + ")");
        options.addOption("i", "interactive", false, "Run in interactive mode");
        options.addOption("d", "demo", false, "Run demonstration of all features");
        
        // Operation options
        options.addOption("o", "operation", true, "Operation to perform");
        options.addOption("p", "path", true, "File or directory path");
        options.addOption("c", "content", true, "File content");
        options.addOption("r", "recursive", false, "Recursive operation");
        options.addOption("s", "stream", false, "Use streaming mode");
        options.addOption("cmd", "command", true, "Command to execute");
        options.addOption("pattern", true, "Pattern for grep");
        
        return options;
    }
    
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("McpTestClient", options);
        
        System.out.println("\nExamples:");
        System.out.println("  # Interactive mode");
        System.out.println("  java -cp target/classes com.example.mcpstreaming.client.McpTestClient -i");
        System.out.println("  ");
        System.out.println("  # Run demo");
        System.out.println("  java -cp target/classes com.example.mcpstreaming.client.McpTestClient -d");
        System.out.println("  ");
        System.out.println("  # List directory");
        System.out.println("  java -cp target/classes com.example.mcpstreaming.client.McpTestClient -o list_directory -p .");
        System.out.println("  ");
        System.out.println("  # Execute command with streaming");
        System.out.println("  java -cp target/classes com.example.mcpstreaming.client.McpTestClient -o execute_command --command \"ls -la\" -s");
    }
    
    public void runInteractiveMode() {
        System.out.println("=== MCP Streaming Service Test Client ===");
        System.out.println("Connected to: " + baseUrl);
        System.out.println("Type 'help' for available commands, 'quit' to exit");
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("\nmcp> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            
            if (input.equals("quit") || input.equals("exit")) {
                break;
            }
            
            if (input.equals("help")) {
                printInteractiveHelp();
                continue;
            }
            
            try {
                handleInteractiveCommand(input);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        
        scanner.close();
        System.out.println("Goodbye!");
    }
    
    private void printInteractiveHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  list <path>                          - List directory contents");
        System.out.println("  list-stream <path>                   - List directory with streaming");
        System.out.println("  read <path>                          - Read file contents");
        System.out.println("  read-stream <path>                   - Read file with streaming");
        System.out.println("  create <path> <content>              - Create file with content");
        System.out.println("  edit <path> <content>                - Edit file with new content");
        System.out.println("  append <path> <content>              - Append content to file");
        System.out.println("  grep <pattern> <path>                - Search for pattern in files");
        System.out.println("  grep-stream <pattern> <path>         - Search with streaming");
        System.out.println("  cmd <command>                        - Execute command");
        System.out.println("  cmd-stream <command>                 - Execute command with streaming");
        System.out.println("  health                               - Check service health");
        System.out.println("  operations                           - List available operations");
        System.out.println("  help                                 - Show this help");
        System.out.println("  quit                                 - Exit client");
    }
    
    private void handleInteractiveCommand(String input) throws Exception {
        String[] parts = input.split("\\s+", 3);
        String command = parts[0];
        
        switch (command) {
            case "health" -> checkHealth();
            case "operations" -> listOperations();
            case "list" -> {
                String path = parts.length > 1 ? parts[1] : ".";
                listDirectory(path, false);
            }
            case "list-stream" -> {
                String path = parts.length > 1 ? parts[1] : ".";
                listDirectory(path, true);
            }
            case "read" -> {
                if (parts.length < 2) throw new IllegalArgumentException("Usage: read <path>");
                readFile(parts[1], false);
            }
            case "read-stream" -> {
                if (parts.length < 2) throw new IllegalArgumentException("Usage: read-stream <path>");
                readFile(parts[1], true);
            }
            case "create" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Usage: create <path> <content>");
                createFile(parts[1], parts[2]);
            }
            case "edit" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Usage: edit <path> <content>");
                editFile(parts[1], parts[2]);
            }
            case "append" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Usage: append <path> <content>");
                appendFile(parts[1], parts[2]);
            }
            case "grep" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Usage: grep <pattern> <path>");
                grep(parts[1], parts[2], false, false);
            }
            case "grep-stream" -> {
                if (parts.length < 3) throw new IllegalArgumentException("Usage: grep-stream <pattern> <path>");
                grep(parts[1], parts[2], false, true);
            }
            case "cmd" -> {
                if (parts.length < 2) throw new IllegalArgumentException("Usage: cmd <command>");
                String cmd = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                executeCommand(cmd, false);
            }
            case "cmd-stream" -> {
                if (parts.length < 2) throw new IllegalArgumentException("Usage: cmd-stream <command>");
                String cmd = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                executeCommand(cmd, true);
            }
            default -> System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
        }
    }
    
    public void runDemo() throws Exception {
        System.out.println("=== MCP Streaming Service Demo ===");
        System.out.println("Base URL: " + baseUrl);
        
        // Check health
        System.out.println("\n1. Checking service health...");
        checkHealth();
        
        // List operations
        System.out.println("\n2. Listing available operations...");
        listOperations();
        
        // List current directory
        System.out.println("\n3. Listing current directory...");
        listDirectory(".", false);
        
        // List with streaming
        System.out.println("\n4. Listing current directory with streaming...");
        listDirectory(".", true);
        
        // Create a test file
        System.out.println("\n5. Creating a test file...");
        createFile("/tmp/mcp-test.txt", "Hello from MCP streaming service!");
        
        // Read the test file
        System.out.println("\n6. Reading the test file...");
        readFile("/tmp/mcp-test.txt", false);
        
        // Append to the test file
        System.out.println("\n7. Appending to the test file...");
        appendFile("/tmp/mcp-test.txt", "\\nAppended content!");
        
        // Execute a simple command
        System.out.println("\n8. Executing a command...");
        String command = System.getProperty("os.name").toLowerCase().contains("win") ? "dir" : "ls -la";
        executeCommand(command, false);
        
        // Execute command with streaming
        System.out.println("\n9. Executing command with streaming...");
        executeCommand("echo 'Streaming output test'", true);
        
        // Grep test
        System.out.println("\n10. Searching for pattern...");
        grep("MCP", "/tmp", false, false);
        
        System.out.println("\n=== Demo completed successfully! ===");
    }
    
    private void runSingleCommand(CommandLine cmd) throws Exception {
        String operation = cmd.getOptionValue("operation");
        if (operation == null) {
            System.err.println("No operation specified. Use -h for help.");
            return;
        }
        
        boolean stream = cmd.hasOption("stream");
        
        switch (operation) {
            case "list_directory" -> {
                String path = cmd.getOptionValue("path", ".");
                listDirectory(path, stream);
            }
            case "read_file" -> {
                String path = cmd.getOptionValue("path");
                if (path == null) throw new IllegalArgumentException("Path required for read_file");
                readFile(path, stream);
            }
            case "create_file" -> {
                String path = cmd.getOptionValue("path");
                String content = cmd.getOptionValue("content", "");
                if (path == null) throw new IllegalArgumentException("Path required for create_file");
                createFile(path, content);
            }
            case "execute_command" -> {
                String command = cmd.getOptionValue("command");
                if (command == null) throw new IllegalArgumentException("Command required for execute_command");
                executeCommand(command, stream);
            }
            case "grep" -> {
                String pattern = cmd.getOptionValue("pattern");
                String path = cmd.getOptionValue("path", ".");
                if (pattern == null) throw new IllegalArgumentException("Pattern required for grep");
                grep(pattern, path, cmd.hasOption("recursive"), stream);
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }
    
    private void checkHealth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/health"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Health check response: " + response.body());
    }
    
    private void listOperations() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/operations"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Available operations: " + response.body());
    }
    
    private void listDirectory(String path, boolean stream) throws Exception {
        Map<String, Object> parameters = Map.of("path", path);
        McpRequest request = new McpRequest("list_directory", parameters, stream);
        
        if (stream) {
            sendStreamingRequest(request);
        } else {
            sendSingleRequest(request);
        }
    }
    
    private void readFile(String path, boolean stream) throws Exception {
        Map<String, Object> parameters = Map.of("path", path);
        McpRequest request = new McpRequest("read_file", parameters, stream);
        
        if (stream) {
            sendStreamingRequest(request);
        } else {
            sendSingleRequest(request);
        }
    }
    
    private void createFile(String path, String content) throws Exception {
        Map<String, Object> parameters = Map.of("path", path, "content", content);
        McpRequest request = new McpRequest("create_file", parameters);
        sendSingleRequest(request);
    }
    
    private void editFile(String path, String content) throws Exception {
        Map<String, Object> parameters = Map.of("path", path, "content", content);
        McpRequest request = new McpRequest("edit_file", parameters);
        sendSingleRequest(request);
    }
    
    private void appendFile(String path, String content) throws Exception {
        Map<String, Object> parameters = Map.of("path", path, "content", content);
        McpRequest request = new McpRequest("append_file", parameters);
        sendSingleRequest(request);
    }
    
    private void grep(String pattern, String path, boolean recursive, boolean stream) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("pattern", pattern);
        parameters.put("path", path);
        parameters.put("recursive", recursive);
        parameters.put("case_sensitive", true);
        
        McpRequest request = new McpRequest("grep", parameters, stream);
        
        if (stream) {
            sendStreamingRequest(request);
        } else {
            sendSingleRequest(request);
        }
    }
    
    private void executeCommand(String command, boolean stream) throws Exception {
        Map<String, Object> parameters = Map.of("command", command, "timeout_seconds", 30);
        McpRequest request = new McpRequest("execute_command", parameters, stream);
        
        if (stream) {
            sendStreamingRequest(request);
        } else {
            sendSingleRequest(request);
        }
    }
    
    private void sendSingleRequest(McpRequest request) throws Exception {
        String requestJson = objectMapper.writeValueAsString(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/request"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response status: " + response.statusCode());
        System.out.println("Response body: " + formatJson(response.body()));
    }
    
    private void sendStreamingRequest(McpRequest request) throws Exception {
        String requestJson = objectMapper.writeValueAsString(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/stream"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/x-ndjson")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build();
        
        System.out.println("Streaming response:");
        
        CompletableFuture<HttpResponse<String>> responseAsync = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        HttpResponse<String> response = responseAsync.get();
        System.out.println("Response status: " + response.statusCode());
        
        // Parse NDJSON response
        String[] lines = response.body().split("\\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                System.out.println("Stream chunk: " + formatJson(line));
            }
        }
    }
    
    private String formatJson(String json) {
        try {
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return json; // Return original if formatting fails
        }
    }
}