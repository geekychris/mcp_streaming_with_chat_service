package com.example.chatservice.service;

import com.example.chatservice.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final ConversationService conversationService;
    private final OllamaService ollamaService;
    private final McpClientService mcpClientService;
    private final HomeDirectoryService homeDirectoryService;
    private final boolean toolsEnabled;
    private final int maxCallsPerTurn;
    private final int timeoutSeconds;
    
    // Cache for conversation histories
    private final Map<String, List<ChatMessage>> conversationCache = new ConcurrentHashMap<>();
    
    public ChatService(
            ConversationService conversationService,
            OllamaService ollamaService,
            McpClientService mcpClientService,
            HomeDirectoryService homeDirectoryService,
            @Value("${chat.tools.enabled}") boolean toolsEnabled,
            @Value("${chat.tools.max-calls-per-turn}") int maxCallsPerTurn,
            @Value("${chat.tools.timeout-seconds}") int timeoutSeconds) {
        this.conversationService = conversationService;
        this.ollamaService = ollamaService;
        this.mcpClientService = mcpClientService;
        this.homeDirectoryService = homeDirectoryService;
        this.toolsEnabled = toolsEnabled;
        this.maxCallsPerTurn = maxCallsPerTurn;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * Process a chat request and generate a response
     */
    public Mono<ChatResponse> processChat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        
        logger.info("Processing chat request for conversation: {}", request.getConversationId());
        
        return Mono.fromCallable(() -> {
            // Generate conversation ID if not provided
            String conversationId = request.getConversationId() != null ? 
                    request.getConversationId() : UUID.randomUUID().toString();
            
            // Create user message
            ChatMessage userMessage = new ChatMessage(
                    UUID.randomUUID().toString(),
                    "user",
                    request.getMessage(),
                    conversationId
            );
            
            return new ProcessingContext(request, userMessage, conversationId, startTime);
        })
        .flatMap(this::loadConversationHistory)
        .flatMap(this::generateResponse)
        .flatMap(this::handleToolCalls)
        .map(this::createFinalResponse)
        .doOnSuccess(response -> logger.info("Chat processing completed in {}ms", 
                response.getProcessingTimeMs()))
        .doOnError(error -> logger.error("Chat processing failed: {}", error.getMessage()));
    }
    
    /**
     * Get conversation history
     */
    public Mono<List<ChatMessage>> getConversationHistory(String conversationId) {
        return conversationService.getConversationHistory(conversationId);
    }
    
    /**
     * Clear conversation history
     */
    public Mono<Void> clearConversationHistory(String conversationId) {
        return conversationService.clearConversationHistory(conversationId)
                .doOnSuccess(v -> conversationCache.remove(conversationId));
    }
    
    /**
     * Get service health status
     */
    public Mono<Map<String, Object>> getServiceHealth() {
        return Mono.zip(
                ollamaService.checkServiceHealth(),
                mcpClientService.checkServiceHealth()
        ).map(tuple -> {
            boolean ollamaHealthy = tuple.getT1();
            boolean mcpHealthy = tuple.getT2();
            boolean overallHealthy = ollamaHealthy && mcpHealthy;
            
            return Map.of(
                    "status", overallHealthy ? "healthy" : "degraded",
                    "services", Map.of(
                            "ollama", Map.of("healthy", ollamaHealthy),
                            "mcp", Map.of("healthy", mcpHealthy)
                    ),
                    "tools_enabled", toolsEnabled
            );
        });
    }
    
    /**
     * Get available models and tools
     */
    public Mono<Map<String, Object>> getCapabilities() {
        return Mono.zip(
                ollamaService.getAvailableModels(),
                mcpClientService.getAvailableOperations().onErrorReturn(null)
        ).map(tuple -> {
            List<String> models = tuple.getT1();
            Object operations = tuple.getT2();
            
            return Map.of(
                    "models", models,
                    "tools_enabled", toolsEnabled,
                    "mcp_operations", operations != null ? operations : "unavailable",
                    "max_tool_calls_per_turn", maxCallsPerTurn
            );
        });
    }
    
    // Private helper classes and methods
    private static class ProcessingContext {
        final ChatRequest request;
        final ChatMessage userMessage;
        final String conversationId;
        final long startTime;
        List<ChatMessage> conversationHistory;
        List<OllamaModels.Message> ollamaMessages;
        OllamaModels.ChatResponse ollamaResponse;
        List<ToolCallResult> toolCallResults;
        ChatMessage assistantMessage;
        
        ProcessingContext(ChatRequest request, ChatMessage userMessage, String conversationId, long startTime) {
            this.request = request;
            this.userMessage = userMessage;
            this.conversationId = conversationId;
            this.startTime = startTime;
            this.toolCallResults = new ArrayList<>();
        }
    }
    
    private Mono<ProcessingContext> loadConversationHistory(ProcessingContext context) {
        return conversationService.addMessage(context.userMessage)
                .then(conversationService.getConversationHistory(context.conversationId))
                .map(history -> {
                    context.conversationHistory = history;
                    context.ollamaMessages = convertToOllamaMessages(history);
                    return context;
                });
    }
    
    private Mono<ProcessingContext> generateResponse(ProcessingContext context) {
        // Prepare tools if enabled
        List<OllamaModels.Tool> tools = toolsEnabled && context.request.getEnableTools() != Boolean.FALSE ? 
                ollamaService.createMcpTools() : null;
        
        return ollamaService.generateChatResponse(
                context.ollamaMessages,
                tools,
                context.request.getModel(),
                context.request.getTemperature(),
                context.request.getMaxTokens()
        ).map(response -> {
            context.ollamaResponse = response;
            return context;
        });
    }
    
    private Mono<ProcessingContext> handleToolCalls(ProcessingContext context) {
        if (!toolsEnabled || context.ollamaResponse.getMessage() == null) {
            return createAssistantMessage(context);
        }
        
        // Parse tool calls from Ollama response
        List<ToolCall> toolCalls = ollamaService.parseToolCalls(context.ollamaResponse.getMessage());
        
        if (toolCalls.isEmpty()) {
            return createAssistantMessage(context);
        }
        
        logger.info("Executing {} tool calls", toolCalls.size());
        
        // Limit number of tool calls
        if (toolCalls.size() > maxCallsPerTurn) {
            logger.warn("Too many tool calls requested: {}, limiting to {}", toolCalls.size(), maxCallsPerTurn);
            toolCalls = toolCalls.subList(0, maxCallsPerTurn);
        }
        
        // Execute tool calls
        return Flux.fromIterable(toolCalls)
                .flatMap(mcpClientService::executeToolCall)
                .collectList()
                .map(results -> {
                    context.toolCallResults.addAll(results);
                    return context;
                })
                .flatMap(this::generateFinalResponseWithTools);
    }
    
    private Mono<ProcessingContext> createAssistantMessage(ProcessingContext context) {
        return Mono.fromCallable(() -> {
            String content = context.ollamaResponse.getMessage() != null ? 
                    context.ollamaResponse.getMessage().getContent() : 
                    "I apologize, but I couldn't generate a response.";
            
            context.assistantMessage = new ChatMessage(
                    UUID.randomUUID().toString(),
                    "assistant",
                    content,
                    context.conversationId
            );
            
            return context;
        });
    }
    
    private Mono<ProcessingContext> generateFinalResponseWithTools(ProcessingContext context) {
        if (context.toolCallResults.isEmpty()) {
            return createAssistantMessage(context);
        }
        
        // Add tool results to conversation
        OllamaModels.Message toolResultMessage = ollamaService.createToolResultMessage(context.toolCallResults);
        context.ollamaMessages.add(toolResultMessage);
        
        // Generate final response with tool results
        return ollamaService.generateChatResponse(
                context.ollamaMessages,
                null, // No tools for final response
                context.request.getModel(),
                context.request.getTemperature(),
                context.request.getMaxTokens()
        ).flatMap(finalResponse -> {
            String content = finalResponse.getMessage() != null ? 
                    finalResponse.getMessage().getContent() : 
                    "I apologize, but I couldn't process the tool results properly.";
            
            context.assistantMessage = new ChatMessage(
                    UUID.randomUUID().toString(),
                    "assistant",
                    content,
                    context.conversationId
            );
            
            context.assistantMessage.setToolCallResults(context.toolCallResults);
            
            return Mono.just(context);
        });
    }
    
    private ChatResponse createFinalResponse(ProcessingContext context) {
        // Save assistant message to conversation
        conversationService.addMessage(context.assistantMessage).subscribe();
        
        ChatResponse response = new ChatResponse(context.assistantMessage, context.conversationId);
        response.setModelUsed(context.ollamaResponse.getModel());
        response.setToolCallsMade(context.toolCallResults);
        response.setProcessingTimeMs(System.currentTimeMillis() - context.startTime);
        
        return response;
    }
    
    private List<OllamaModels.Message> convertToOllamaMessages(List<ChatMessage> chatMessages) {
        List<OllamaModels.Message> ollamaMessages = new ArrayList<>();
        
        // Add system message at the beginning of new conversations to provide context
        if (chatMessages.isEmpty() || !chatMessages.get(0).getRole().equals("system")) {
            String homeDir = homeDirectoryService.getHomeDirectory();
            ollamaMessages.add(new OllamaModels.Message("system", 
                "You are an AI assistant with access to powerful tools for file operations and system commands. " +
                "Important system context: " +
                "- The current user's home directory is " + homeDir + " " +
                "- Use absolute paths when possible " +
                "- When users ask for 'my home directory' or 'home directory', use " + homeDir + " " +
                "- Common paths: /Applications for apps, /tmp for temp files, " + homeDir + " for user home " +
                "Always use the available tools to help users with file operations, system commands, and information gathering."
            ));
        }
        
        // Add all existing messages
        chatMessages.stream()
                .map(msg -> new OllamaModels.Message(msg.getRole(), msg.getContent()))
                .forEach(ollamaMessages::add);
                
        return ollamaMessages;
    }
}