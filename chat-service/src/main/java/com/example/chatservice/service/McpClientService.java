package com.example.chatservice.service;

import com.example.chatservice.model.ToolCall;
import com.example.chatservice.model.ToolCallResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class McpClientService {
    private static final Logger logger = LoggerFactory.getLogger(McpClientService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final int retryDelaySeconds;
    private final HomeDirectoryService homeDirectoryService;
    
    public McpClientService(
            @Value("${chat.mcp.service-url}") String serviceUrl,
            @Value("${chat.mcp.timeout-seconds}") int timeoutSeconds,
            @Value("${chat.mcp.max-retries}") int maxRetries,
            @Value("${chat.mcp.retry-delay-seconds}") int retryDelaySeconds,
            HomeDirectoryService homeDirectoryService) {
        this.webClient = WebClient.builder()
                .baseUrl(serviceUrl)
                .build();
        this.objectMapper = new ObjectMapper();
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.retryDelaySeconds = retryDelaySeconds;
        this.homeDirectoryService = homeDirectoryService;
    }
    
    /**
     * Execute a tool call using the MCP service
     */
    public Mono<ToolCallResult> executeToolCall(ToolCall toolCall) {
        logger.info("Executing tool call: {} with parameters: {}", toolCall.getName(), toolCall.getParameters());
        
        // Translate parameters for macOS environment
        Map<String, Object> translatedParameters = translateParameters(toolCall.getParameters());
        
        // Create MCP request with proper format
        Map<String, Object> mcpRequest = Map.of(
                "type", "request",
                "operation", toolCall.getName(),
                "parameters", translatedParameters,
                "stream", false
        );
        
        return webClient.post()
                .uri("/api/mcp/request")
                .bodyValue(mcpRequest)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(retryDelaySeconds)))
                .map(response -> processResponse(toolCall, response))
                .doOnSuccess(result -> logger.info("Tool call {} completed: {}", toolCall.getName(), result.isSuccess()))
                .doOnError(error -> logger.error("Tool call {} failed: {}", toolCall.getName(), error.getMessage()))
                .onErrorReturn(ToolCallResult.error(
                        toolCall.getId(), 
                        toolCall.getName(), 
                        "Tool call failed due to service error"
                ));
    }
    
    /**
     * Get available MCP operations
     */
    public Mono<JsonNode> getAvailableOperations() {
        logger.info("Fetching available MCP operations");
        
        return webClient.get()
                .uri("/api/mcp/operations")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(retryDelaySeconds)))
                .doOnSuccess(operations -> logger.info("Retrieved {} operations", operations.size()))
                .doOnError(error -> logger.error("Failed to get operations: {}", error.getMessage()));
    }
    
    /**
     * Check MCP service health
     */
    public Mono<Boolean> checkServiceHealth() {
        return webClient.get()
                .uri("/api/mcp/health")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    if (response.has("status")) {
                        String status = response.get("status").asText();
                        return "healthy".equals(status) || "UP".equals(status);
                    }
                    return false;
                })
                .doOnSuccess(healthy -> logger.info("MCP service health check: {}", healthy ? "healthy" : "unhealthy"))
                .doOnError(error -> logger.warn("MCP service health check failed: {}", error.getMessage()))
                .onErrorReturn(false);
    }
    
    private ToolCallResult processResponse(ToolCall toolCall, JsonNode response) {
        try {
            // Handle MCP service response format: {"type": "response", "status": "success", "result": ...}
            if (response.has("type") && "response".equals(response.get("type").asText())) {
                if (response.has("status") && "success".equals(response.get("status").asText())) {
                    Object result = response.has("result") ? 
                            objectMapper.convertValue(response.get("result"), Object.class) : 
                            null;
                    return ToolCallResult.success(toolCall.getId(), toolCall.getName(), result);
                } else {
                    String errorMessage = response.has("error_message") ? 
                            response.get("error_message").asText() : 
                            "MCP operation failed";
                    return ToolCallResult.error(toolCall.getId(), toolCall.getName(), errorMessage);
                }
            } else if (response.has("type") && "error".equals(response.get("type").asText())) {
                String errorMessage = response.has("message") ? 
                        response.get("message").asText() : 
                        "MCP error occurred";
                return ToolCallResult.error(toolCall.getId(), toolCall.getName(), errorMessage);
            } else {
                // Fallback for unexpected response format
                logger.warn("Unexpected MCP response format: {}", response.toString());
                return ToolCallResult.error(toolCall.getId(), toolCall.getName(), "Unexpected response format from MCP service");
            }
        } catch (Exception e) {
            logger.error("Error processing response for tool {}: {}", toolCall.getName(), e.getMessage());
            return ToolCallResult.error(toolCall.getId(), toolCall.getName(), "Failed to process response");
        }
    }
    
    /**
     * Create a tool call for listing directory
     */
    public ToolCall createListDirectoryCall(String path) {
        String id = UUID.randomUUID().toString();
        return new ToolCall(id, "list_directory", Map.of("path", path));
    }
    
    /**
     * Create a tool call for reading a file
     */
    public ToolCall createReadFileCall(String path) {
        String id = UUID.randomUUID().toString();
        return new ToolCall(id, "read_file", Map.of("path", path));
    }
    
    /**
     * Create a tool call for creating a file
     */
    public ToolCall createCreateFileCall(String path, String content) {
        String id = UUID.randomUUID().toString();
        return new ToolCall(id, "create_file", Map.of("path", path, "content", content));
    }
    
    /**
     * Create a tool call for editing a file
     */
    public ToolCall createEditFileCall(String path, String content) {
        String id = UUID.randomUUID().toString();
        return new ToolCall(id, "edit_file", Map.of("path", path, "content", content));
    }
    
    /**
     * Create a tool call for appending to a file
     */
    public ToolCall createAppendFileCall(String path, String content) {
        String id = UUID.randomUUID().toString();
        return new ToolCall(id, "append_file", Map.of("path", path, "content", content));
    }
    
    /**
     * Create a tool call for executing a command
     */
    public ToolCall createExecuteCommandCall(String command, String workingDirectory) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> params = Map.of(
                "command", command,
                "working_directory", workingDirectory != null ? workingDirectory : ".",
                "timeout_seconds", 30
        );
        return new ToolCall(id, "execute_command", params);
    }
    
    /**
     * Create a tool call for grep search
     */
    public ToolCall createGrepCall(String pattern, String path, boolean recursive) {
        String id = UUID.randomUUID().toString();
        return new ToolCall(id, "grep", Map.of(
                "pattern", pattern,
                "path", path,
                "recursive", recursive,
                "case_sensitive", false
        ));
    }
    
    /**
     * Translate parameters to handle macOS-specific paths and common aliases
     */
    private Map<String, Object> translateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return Map.of();
        }
        
        Map<String, Object> translated = new HashMap<>(parameters);
        
        // Handle path parameter translation
        if (translated.containsKey("path")) {
            String path = translated.get("path").toString();
            String translatedPath = translatePath(path);
            translated.put("path", translatedPath);
            logger.info("Translated path '{}' to '{}'", path, translatedPath);
        }
        
        return translated;
    }
    
    /**
     * Translate common path aliases to actual system paths
     */
    private String translatePath(String path) {
        if (path == null) {
            return ".";
        }
        
        String homeDir = homeDirectoryService.getHomeDirectory();
        
        // Handle common home directory aliases
        if ("/home".equals(path) || "/home/".equals(path)) {
            return homeDir;
        }
        if (path.startsWith("/home/")) {
            // Replace /home/username with actual home directory
            return path.replaceFirst("/home/[^/]*", homeDir);
        }
        
        // Handle tilde expansion
        return homeDirectoryService.expandTildePath(path);
    }
}
