package com.example.chatservice.service;

import com.example.chatservice.model.OllamaModels;
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

import java.time.Duration;
import java.util.*;

@Service
public class OllamaService {
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String defaultModel;
    private final int timeoutSeconds;
    private final double defaultTemperature;
    private final int defaultMaxTokens;
    
    public OllamaService(
            @Value("${chat.ollama.base-url}") String baseUrl,
            @Value("${chat.ollama.default-model}") String defaultModel,
            @Value("${chat.ollama.timeout-seconds}") int timeoutSeconds,
            @Value("${chat.ollama.temperature}") double defaultTemperature,
            @Value("${chat.ollama.max-tokens}") int defaultMaxTokens) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = new ObjectMapper();
        this.defaultModel = defaultModel;
        this.timeoutSeconds = timeoutSeconds;
        this.defaultTemperature = defaultTemperature;
        this.defaultMaxTokens = defaultMaxTokens;
    }
    
    /**
     * Generate chat response using Ollama
     */
    public Mono<OllamaModels.ChatResponse> generateChatResponse(
            List<OllamaModels.Message> messages, 
            List<OllamaModels.Tool> tools,
            String model,
            Double temperature,
            Integer maxTokens) {
        
        logger.info("Generating chat response with model: {} for {} messages", 
                model != null ? model : defaultModel, messages.size());
        
        OllamaModels.ChatRequest request = new OllamaModels.ChatRequest();
        request.setModel(model != null ? model : defaultModel);
        request.setMessages(messages);
        request.setTools(tools);
        request.setStream(false);
        
        // Set options
        OllamaModels.Options options = new OllamaModels.Options();
        options.setTemperature(temperature != null ? temperature : defaultTemperature);
        options.setNumPredict(maxTokens != null ? maxTokens : defaultMaxTokens);
        request.setOptions(options);
        
        return webClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaModels.ChatResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> logger.info("Chat response generated successfully"))
                .doOnError(error -> logger.error("Failed to generate chat response: {}", error.getMessage()));
    }
    
    /**
     * Check if Ollama service is available
     */
    public Mono<Boolean> checkServiceHealth() {
        return webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> response.has("models"))
                .doOnSuccess(healthy -> logger.info("Ollama service health check: {}", healthy ? "healthy" : "unhealthy"))
                .doOnError(error -> logger.warn("Ollama service health check failed: {}", error.getMessage()))
                .onErrorReturn(false);
    }
    
    /**
     * Get available models from Ollama
     */
    public Mono<List<String>> getAvailableModels() {
        return webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(10))
                .map(this::extractModelNames)
                .doOnSuccess(models -> logger.info("Found {} available models", models.size()))
                .doOnError(error -> logger.error("Failed to get available models: {}", error.getMessage()))
                .onErrorReturn(List.of());
    }
    
    /**
     * Create MCP tools for Ollama function calling
     */
    public List<OllamaModels.Tool> createMcpTools() {
        List<OllamaModels.Tool> tools = new ArrayList<>();
        
        // File system operations
        tools.add(createTool(
                "list_directory",
                "List files and directories in a given path",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The directory path to list"
                                )
                        ),
                        "required", List.of("path")
                )
        ));
        
        tools.add(createTool(
                "read_file",
                "Read the contents of a file",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The file path to read"
                                )
                        ),
                        "required", List.of("path")
                )
        ));
        
        tools.add(createTool(
                "create_file",
                "Create a new file with specified content",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The file path to create"
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "The content to write to the file"
                                )
                        ),
                        "required", List.of("path", "content")
                )
        ));
        
        tools.add(createTool(
                "edit_file",
                "Edit an existing file by replacing its content",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The file path to edit"
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "The new content for the file"
                                )
                        ),
                        "required", List.of("path", "content")
                )
        ));
        
        tools.add(createTool(
                "append_file",
                "Append content to an existing file",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The file path to append to"
                                ),
                                "content", Map.of(
                                        "type", "string",
                                        "description", "The content to append"
                                )
                        ),
                        "required", List.of("path", "content")
                )
        ));
        
        // Command execution
        tools.add(createTool(
                "execute_command",
                "Execute a system command",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "command", Map.of(
                                        "type", "string",
                                        "description", "The command to execute"
                                ),
                                "working_directory", Map.of(
                                        "type", "string",
                                        "description", "The working directory (optional, defaults to current directory)"
                                )
                        ),
                        "required", List.of("command")
                )
        ));
        
        // Search operations
        tools.add(createTool(
                "grep",
                "Search for patterns in files or directories",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "pattern", Map.of(
                                        "type", "string",
                                        "description", "The search pattern (regex)"
                                ),
                                "path", Map.of(
                                        "type", "string",
                                        "description", "The file or directory path to search in"
                                ),
                                "recursive", Map.of(
                                        "type", "boolean",
                                        "description", "Whether to search recursively (default: true)"
                                )
                        ),
                        "required", List.of("pattern", "path")
                )
        ));
        
        return tools;
    }
    
    /**
     * Parse tool calls from Ollama response
     */
    public List<ToolCall> parseToolCalls(OllamaModels.Message message) {
        if (message.getToolCalls() == null || message.getToolCalls().isEmpty()) {
            return List.of();
        }
        
        List<ToolCall> toolCalls = new ArrayList<>();
        for (OllamaModels.ToolCallData toolCallData : message.getToolCalls()) {
            try {
                OllamaModels.FunctionCall functionCall = toolCallData.getFunction();
                if (functionCall != null) {
                    String id = UUID.randomUUID().toString();
                    String name = functionCall.getName();
                    
                    // Handle arguments as either Object or String
                    Map<String, Object> parameters;
                    Object args = functionCall.getArguments();
                    if (args instanceof Map) {
                        parameters = (Map<String, Object>) args;
                    } else if (args instanceof String) {
                        // Parse arguments JSON string to Map
                        parameters = objectMapper.readValue(
                                (String) args, 
                                Map.class
                        );
                    } else {
                        parameters = Map.of();
                    }
                    
                    toolCalls.add(new ToolCall(id, name, parameters));
                }
            } catch (Exception e) {
                logger.error("Failed to parse tool call: {}", e.getMessage());
            }
        }
        
        return toolCalls;
    }
    
    /**
     * Create a tool result message for Ollama
     */
    public OllamaModels.Message createToolResultMessage(List<ToolCallResult> results) {
        StringBuilder content = new StringBuilder("Tool execution results:\n");
        
        for (ToolCallResult result : results) {
            content.append(String.format("- %s: ", result.getToolName()));
            if (result.isSuccess()) {
                content.append("SUCCESS - ");
                if (result.getResult() != null) {
                    content.append(result.getResult().toString());
                }
            } else {
                content.append("ERROR - ").append(result.getError());
            }
            content.append("\n");
        }
        
        return new OllamaModels.Message("tool", content.toString());
    }
    
    private OllamaModels.Tool createTool(String name, String description, Map<String, Object> parameters) {
        OllamaModels.Function function = new OllamaModels.Function(name, description, parameters);
        return new OllamaModels.Tool(function);
    }
    
    private List<String> extractModelNames(JsonNode response) {
        List<String> modelNames = new ArrayList<>();
        if (response.has("models")) {
            JsonNode models = response.get("models");
            if (models.isArray()) {
                for (JsonNode model : models) {
                    if (model.has("name")) {
                        modelNames.add(model.get("name").asText());
                    }
                }
            }
        }
        return modelNames;
    }
}