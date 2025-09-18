package com.example.chatservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class ChatMessage {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("role")
    private String role; // "user", "assistant", "system"
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("conversation_id")
    private String conversationId;
    
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
    
    @JsonProperty("tool_call_results")
    private List<ToolCallResult> toolCallResults;
    
    public ChatMessage() {
        this.timestamp = Instant.now();
    }
    
    public ChatMessage(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }
    
    public ChatMessage(String id, String role, String content, String conversationId) {
        this(role, content);
        this.id = id;
        this.conversationId = conversationId;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }
    
    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }
    
    public List<ToolCallResult> getToolCallResults() {
        return toolCallResults;
    }
    
    public void setToolCallResults(List<ToolCallResult> toolCallResults) {
        this.toolCallResults = toolCallResults;
    }
}