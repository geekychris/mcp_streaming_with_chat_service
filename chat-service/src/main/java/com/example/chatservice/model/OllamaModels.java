package com.example.chatservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class OllamaModels {
    
    public static class ChatRequest {
        @JsonProperty("model")
        private String model;
        
        @JsonProperty("messages")
        private List<Message> messages;
        
        @JsonProperty("tools")
        private List<Tool> tools;
        
        @JsonProperty("stream")
        private boolean stream = false;
        
        @JsonProperty("options")
        private Options options;
        
        public ChatRequest() {}
        
        public ChatRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
        
        // Getters and setters
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
        public List<Tool> getTools() { return tools; }
        public void setTools(List<Tool> tools) { this.tools = tools; }
        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
        public Options getOptions() { return options; }
        public void setOptions(Options options) { this.options = options; }
    }
    
    public static class ChatResponse {
        @JsonProperty("model")
        private String model;
        
        @JsonProperty("created_at")
        private String createdAt;
        
        @JsonProperty("message")
        private Message message;
        
        @JsonProperty("done")
        private boolean done;
        
        @JsonProperty("total_duration")
        private Long totalDuration;
        
        @JsonProperty("load_duration")
        private Long loadDuration;
        
        @JsonProperty("prompt_eval_count")
        private Integer promptEvalCount;
        
        @JsonProperty("prompt_eval_duration")
        private Long promptEvalDuration;
        
        @JsonProperty("eval_count")
        private Integer evalCount;
        
        @JsonProperty("eval_duration")
        private Long evalDuration;
        
        // Getters and setters
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        public boolean isDone() { return done; }
        public void setDone(boolean done) { this.done = done; }
        public Long getTotalDuration() { return totalDuration; }
        public void setTotalDuration(Long totalDuration) { this.totalDuration = totalDuration; }
        public Long getLoadDuration() { return loadDuration; }
        public void setLoadDuration(Long loadDuration) { this.loadDuration = loadDuration; }
        public Integer getPromptEvalCount() { return promptEvalCount; }
        public void setPromptEvalCount(Integer promptEvalCount) { this.promptEvalCount = promptEvalCount; }
        public Long getPromptEvalDuration() { return promptEvalDuration; }
        public void setPromptEvalDuration(Long promptEvalDuration) { this.promptEvalDuration = promptEvalDuration; }
        public Integer getEvalCount() { return evalCount; }
        public void setEvalCount(Integer evalCount) { this.evalCount = evalCount; }
        public Long getEvalDuration() { return evalDuration; }
        public void setEvalDuration(Long evalDuration) { this.evalDuration = evalDuration; }
    }
    
    public static class Message {
        @JsonProperty("role")
        private String role;
        
        @JsonProperty("content")
        private String content;
        
        @JsonProperty("tool_calls")
        private List<ToolCallData> toolCalls;
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        // Getters and setters
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<ToolCallData> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCallData> toolCalls) { this.toolCalls = toolCalls; }
    }
    
    public static class Tool {
        @JsonProperty("type")
        private String type = "function";
        
        @JsonProperty("function")
        private Function function;
        
        public Tool() {}
        
        public Tool(Function function) {
            this.function = function;
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Function getFunction() { return function; }
        public void setFunction(Function function) { this.function = function; }
    }
    
    public static class Function {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("parameters")
        private Map<String, Object> parameters;
        
        public Function() {}
        
        public Function(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
    
    public static class ToolCallData {
        @JsonProperty("function")
        private FunctionCall function;
        
        public ToolCallData() {}
        
        public ToolCallData(FunctionCall function) {
            this.function = function;
        }
        
        // Getters and setters
        public FunctionCall getFunction() { return function; }
        public void setFunction(FunctionCall function) { this.function = function; }
    }
    
    public static class FunctionCall {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("arguments")
        private Object arguments;
        
        public FunctionCall() {}
        
        public FunctionCall(String name, Object arguments) {
            this.name = name;
            this.arguments = arguments;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Object getArguments() { return arguments; }
        public void setArguments(Object arguments) { this.arguments = arguments; }
    }
    
    public static class Options {
        @JsonProperty("temperature")
        private Double temperature;
        
        @JsonProperty("num_predict")
        private Integer numPredict;
        
        public Options() {}
        
        public Options(Double temperature, Integer numPredict) {
            this.temperature = temperature;
            this.numPredict = numPredict;
        }
        
        // Getters and setters
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getNumPredict() { return numPredict; }
        public void setNumPredict(Integer numPredict) { this.numPredict = numPredict; }
    }
}