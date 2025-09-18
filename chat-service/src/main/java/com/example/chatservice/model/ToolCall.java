package com.example.chatservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class ToolCall {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("parameters")
    private Map<String, Object> parameters;
    
    public ToolCall() {}
    
    public ToolCall(String id, String name, Map<String, Object> parameters) {
        this.id = id;
        this.name = name;
        this.parameters = parameters;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}