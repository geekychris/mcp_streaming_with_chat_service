package com.example.mcpstreaming.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all MCP messages.
 * Implements the core MCP protocol message structure with polymorphic serialization.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpRequest.class, name = "request"),
    @JsonSubTypes.Type(value = McpResponse.class, name = "response"),
    @JsonSubTypes.Type(value = McpStreamChunk.class, name = "stream_chunk"),
    @JsonSubTypes.Type(value = McpError.class, name = "error")
})
public abstract class McpMessage {
    
    @NotNull
    @JsonProperty("id")
    private String id;
    
    @NotNull
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @NotNull
    @JsonProperty("version")
    private String version = "1.0";
    
    public McpMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }
    
    public McpMessage(String id) {
        this.id = id;
        this.timestamp = Instant.now();
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}