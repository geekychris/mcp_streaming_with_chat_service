package com.example.mcpstreaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * MCP Response message for operation results.
 */
public class McpResponse extends McpMessage {
    
    @NotNull
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("status")
    private String status = "success";
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("stream_complete")
    private boolean streamComplete = true;
    
    public McpResponse() {
        super();
    }
    
    public McpResponse(String requestId, Object result) {
        super();
        this.requestId = requestId;
        this.result = result;
    }
    
    public McpResponse(String requestId, String status, Object result) {
        super();
        this.requestId = requestId;
        this.status = status;
        this.result = result;
    }
    
    // Getters and setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    
    public boolean isStreamComplete() { return streamComplete; }
    public void setStreamComplete(boolean streamComplete) { this.streamComplete = streamComplete; }
}