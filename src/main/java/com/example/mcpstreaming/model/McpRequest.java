package com.example.mcpstreaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * MCP Request message for initiating operations.
 */
public class McpRequest extends McpMessage {
    
    @NotNull
    @JsonProperty("operation")
    private String operation;
    
    @JsonProperty("parameters")
    private Map<String, Object> parameters;
    
    @JsonProperty("stream")
    private boolean stream = false;
    
    public McpRequest() {
        super();
    }
    
    public McpRequest(String operation, Map<String, Object> parameters) {
        super();
        this.operation = operation;
        this.parameters = parameters;
    }
    
    public McpRequest(String operation, Map<String, Object> parameters, boolean stream) {
        super();
        this.operation = operation;
        this.parameters = parameters;
        this.stream = stream;
    }
    
    // Getters and setters
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
}