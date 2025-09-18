package com.example.mcpstreaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * MCP Stream Chunk message for streaming data.
 */
public class McpStreamChunk extends McpMessage {
    
    @NotNull
    @JsonProperty("request_id")
    private String requestId;
    
    @JsonProperty("sequence")
    private long sequence;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("is_final")
    private boolean isFinal = false;
    
    public McpStreamChunk() {
        super();
    }
    
    public McpStreamChunk(String requestId, long sequence, Object data) {
        super();
        this.requestId = requestId;
        this.sequence = sequence;
        this.data = data;
    }
    
    public McpStreamChunk(String requestId, long sequence, Object data, boolean isFinal) {
        super();
        this.requestId = requestId;
        this.sequence = sequence;
        this.data = data;
        this.isFinal = isFinal;
    }
    
    // Getters and setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
    
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    
    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
}