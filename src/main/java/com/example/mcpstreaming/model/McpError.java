package com.example.mcpstreaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * MCP Error message for error conditions.
 */
public class McpError extends McpMessage {
    
    @JsonProperty("request_id")
    private String requestId;
    
    @NotNull
    @JsonProperty("error_code")
    private String errorCode;
    
    @NotNull
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("details")
    private Object details;
    
    public McpError() {
        super();
    }
    
    public McpError(String errorCode, String errorMessage) {
        super();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public McpError(String requestId, String errorCode, String errorMessage) {
        super();
        this.requestId = requestId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public McpError(String requestId, String errorCode, String errorMessage, Object details) {
        super();
        this.requestId = requestId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.details = details;
    }
    
    // Getters and setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
}