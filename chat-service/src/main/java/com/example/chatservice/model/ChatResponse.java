package com.example.chatservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class ChatResponse {
    @JsonProperty("message")
    private ChatMessage message;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("model_used")
    private String modelUsed;
    
    @JsonProperty("tool_calls_made")
    private List<ToolCallResult> toolCallsMade;
    
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    public ChatResponse() {
        this.timestamp = Instant.now();
    }
    
    public ChatResponse(ChatMessage message, String conversationId) {
        this();
        this.message = message;
        this.conversationId = conversationId;
    }
    
    // Getters and setters
    public ChatMessage getMessage() {
        return message;
    }
    
    public void setMessage(ChatMessage message) {
        this.message = message;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getModelUsed() {
        return modelUsed;
    }
    
    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }
    
    public List<ToolCallResult> getToolCallsMade() {
        return toolCallsMade;
    }
    
    public void setToolCallsMade(List<ToolCallResult> toolCallsMade) {
        this.toolCallsMade = toolCallsMade;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}