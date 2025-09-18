package com.example.chatservice.service;

import com.example.chatservice.model.ChatMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ConversationService {
    
    // In-memory storage for conversations (in production, use a database)
    private final ConcurrentHashMap<String, List<ChatMessage>> conversations = new ConcurrentHashMap<>();
    
    /**
     * Add a message to a conversation
     */
    public Mono<Void> addMessage(ChatMessage message) {
        return Mono.fromRunnable(() -> {
            String conversationId = message.getConversationId();
            conversations.computeIfAbsent(conversationId, k -> new CopyOnWriteArrayList<>())
                    .add(message);
        });
    }
    
    /**
     * Get conversation history
     */
    public Mono<List<ChatMessage>> getConversationHistory(String conversationId) {
        return Mono.fromSupplier(() -> 
                conversations.getOrDefault(conversationId, List.of())
        );
    }
    
    /**
     * Clear conversation history
     */
    public Mono<Void> clearConversationHistory(String conversationId) {
        return Mono.fromRunnable(() -> conversations.remove(conversationId));
    }
    
    /**
     * Get all active conversation IDs
     */
    public Mono<List<String>> getActiveConversations() {
        return Mono.fromSupplier(() -> List.copyOf(conversations.keySet()));
    }
    
    /**
     * Get conversation count
     */
    public Mono<Integer> getConversationCount() {
        return Mono.fromSupplier(() -> conversations.size());
    }
}