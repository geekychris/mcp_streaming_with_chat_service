package com.example.chatservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ToolCallResult {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("tool_name")
    private String toolName;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private String error;
    
    public ToolCallResult() {}
    
    public ToolCallResult(String id, String toolName, boolean success, Object result, String error) {
        this.id = id;
        this.toolName = toolName;
        this.success = success;
        this.result = result;
        this.error = error;
    }
    
    public static ToolCallResult success(String id, String toolName, Object result) {
        return new ToolCallResult(id, toolName, true, result, null);
    }
    
    public static ToolCallResult error(String id, String toolName, String error) {
        return new ToolCallResult(id, toolName, false, null, error);
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}