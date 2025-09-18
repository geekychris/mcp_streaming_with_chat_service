package com.example.chatservice.controller;

import com.example.chatservice.model.ChatMessage;
import com.example.chatservice.model.ChatRequest;
import com.example.chatservice.model.ChatResponse;
import com.example.chatservice.service.ChatService;
import com.example.chatservice.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Enable CORS for frontend integration
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;
    private final ConversationService conversationService;
    
    public ChatController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }
    
    /**
     * Send a chat message and get response
     */
    @PostMapping("/message")
    public Mono<ResponseEntity<ChatResponse>> sendMessage(@Valid @RequestBody ChatRequest request) {
        logger.info("Received chat message: {} chars", request.getMessage().length());
        
        return chatService.processChat(request)
                .map(response -> ResponseEntity.ok(response))
                .doOnSuccess(response -> logger.info("Chat response sent for conversation: {}", 
                        response.getBody().getConversationId()))
                .onErrorResume(error -> {
                    logger.error("Failed to process chat message: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build());
                });
    }
    
    /**
     * Get conversation history
     */
    @GetMapping("/conversation/{conversationId}/history")
    public Mono<ResponseEntity<List<ChatMessage>>> getConversationHistory(
            @PathVariable String conversationId) {
        logger.info("Getting history for conversation: {}", conversationId);
        
        return chatService.getConversationHistory(conversationId)
                .map(history -> ResponseEntity.ok(history))
                .onErrorResume(error -> {
                    logger.error("Failed to get conversation history: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build());
                });
    }
    
    /**
     * Clear conversation history
     */
    @DeleteMapping("/conversation/{conversationId}")
    public Mono<ResponseEntity<Map<String, String>>> clearConversation(
            @PathVariable String conversationId) {
        logger.info("Clearing conversation: {}", conversationId);
        
        return chatService.clearConversationHistory(conversationId)
                .then(Mono.just(ResponseEntity.ok(Map.of(
                        "message", "Conversation cleared successfully",
                        "conversation_id", conversationId
                ))))
                .onErrorResume(error -> {
                    logger.error("Failed to clear conversation: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to clear conversation")));
                });
    }
    
    /**
     * Get all active conversations
     */
    @GetMapping("/conversations")
    public Mono<ResponseEntity<Map<String, Object>>> getActiveConversations() {
        logger.info("Getting active conversations");
        
        return conversationService.getActiveConversations()
                .zipWith(conversationService.getConversationCount())
                .map(tuple -> {
                    List<String> conversations = tuple.getT1();
                    Integer count = tuple.getT2();
                    
                    return ResponseEntity.ok(Map.of(
                            "conversations", conversations,
                            "count", count
                    ));
                })
                .onErrorResume(error -> {
                    logger.error("Failed to get active conversations: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build());
                });
    }
    
    /**
     * Get service health status
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> getHealth() {
        return chatService.getServiceHealth()
                .map(health -> {
                    boolean isHealthy = "healthy".equals(health.get("status"));
                    HttpStatus status = isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
                    return ResponseEntity.status(status).body(health);
                })
                .onErrorResume(error -> {
                    logger.error("Health check failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(Map.of(
                                    "status", "error",
                                    "error", error.getMessage()
                            )));
                });
    }
    
    /**
     * Get service capabilities (models, tools, etc.)
     */
    @GetMapping("/capabilities")
    public Mono<ResponseEntity<Map<String, Object>>> getCapabilities() {
        return chatService.getCapabilities()
                .map(capabilities -> ResponseEntity.ok(capabilities))
                .onErrorResume(error -> {
                    logger.error("Failed to get capabilities: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to get capabilities")));
                });
    }
    
    /**
     * Simple ping endpoint for basic connectivity test
     */
    @GetMapping("/ping")
    public Mono<ResponseEntity<Map<String, Object>>> ping() {
        return Mono.just(ResponseEntity.ok(Map.of(
                "message", "Chat service is running",
                "timestamp", System.currentTimeMillis(),
                "service", "llama-chat-service"
        )));
    }
}