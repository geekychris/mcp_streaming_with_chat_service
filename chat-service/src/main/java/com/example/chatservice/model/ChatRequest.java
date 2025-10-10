package com.example.chatservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    @JsonProperty("message")
    @NotBlank(message = "Message cannot be blank")
    private String message;
    
    @JsonAlias("conversation_id")
    private String conversationId;
    
    @JsonProperty("model")
    private String model; // Optional override for default model
    
    private Boolean enableTools = true;
    
    @JsonProperty("temperature")
    private Double temperature;
    
    private Integer maxTokens;
    
    public ChatRequest() {}
    
    public ChatRequest(String message) {
        this.message = message;
    }
    
    public ChatRequest(String message, String conversationId) {
        this.message = message;
        this.conversationId = conversationId;
    }
    
    // Getters and setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Boolean getEnableTools() {
        return enableTools;
    }
    
    public void setEnableTools(Boolean enableTools) {
        this.enableTools = enableTools;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}