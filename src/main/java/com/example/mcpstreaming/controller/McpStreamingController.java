package com.example.mcpstreaming.controller;

import com.example.mcpstreaming.api.HealthApi;
import com.example.mcpstreaming.api.McpOperationsApi;
import com.example.mcpstreaming.api.model.HealthResponse;
import com.example.mcpstreaming.api.model.ListOperations200Response;
import com.example.mcpstreaming.model.*;
import com.example.mcpstreaming.service.CommandExecutionService;
import com.example.mcpstreaming.service.FileOperationService;
import com.example.mcpstreaming.service.GrepService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST controller for handling MCP streaming requests and responses.
 * Implements the MCP protocol over HTTP with support for both single responses and streaming.
 */
@RestController
public class McpStreamingController implements McpOperationsApi, HealthApi {
    
    private static final Logger logger = LoggerFactory.getLogger(McpStreamingController.class);
    
    @Autowired
    private FileOperationService fileOperationService;
    
    @Autowired
    private GrepService grepService;
    
    @Autowired
    private CommandExecutionService commandExecutionService;
    
    /**
     * Handles MCP requests and returns appropriate responses.
     */
    @Override
    public Mono<ResponseEntity<Object>> handleRequest(
            Mono<com.example.mcpstreaming.api.model.McpRequest> mcpRequestMono, ServerWebExchange exchange) {
        return mcpRequestMono.flatMap(apiRequest -> {
            // Convert API model to internal model
            McpRequest request = convertApiRequestToInternal(apiRequest);
            logger.info("Received MCP request: operation={}, stream={}", request.getOperation(), request.isStream());
            
            try {
                if (request.isStream()) {
                    // For streaming requests, return an initial response indicating streaming will begin
                    McpResponse response = new McpResponse(request.getId(), "streaming", "Stream initiated");
                    response.setStreamComplete(false);
                    Object apiMessage = convertInternalToApiDetailedResponse(response);
                    return Mono.just(ResponseEntity.ok(apiMessage));
                } else {
                    // Handle non-streaming request
                    return handleNonStreamingRequest(request)
                        .map(msg -> ResponseEntity.ok(convertInternalToApiDetailedResponse(msg)));
                }
            } catch (Exception e) {
                logger.error("Error handling MCP request", e);
                McpError error = new McpError(
                    request.getId(),
                    "REQUEST_ERROR",
                    "Error processing request: " + e.getMessage(),
                    e.getClass().getSimpleName()
                );
                return Mono.just(ResponseEntity.ok(convertInternalToApiDetailedResponse(error)));
            }
        });
    }
    
    /**
     * Handles streaming MCP requests and returns a stream of chunks.
     */
    @Override
    public Mono<ResponseEntity<Flux<Object>>> handleStreamingRequest(
            Mono<com.example.mcpstreaming.api.model.McpRequest> mcpRequestMono, ServerWebExchange exchange) {
        return mcpRequestMono.map(apiRequest -> {
            // Convert API model to internal model
            McpRequest request = convertApiRequestToInternal(apiRequest);
            logger.info("Received MCP streaming request: operation={}", request.getOperation());
            
            try {
                Flux<Object> streamFlux = handleStreamingOperation(request)
                    .map(msg -> this.convertInternalToApiDetailedResponse(msg));
                return ResponseEntity.ok(streamFlux);
            } catch (Exception e) {
                logger.error("Error handling streaming MCP request", e);
                McpError error = new McpError(
                    request.getId(),
                    "STREAM_ERROR",
                    "Error processing streaming request: " + e.getMessage(),
                    e.getClass().getSimpleName()
                );
                return ResponseEntity.ok(Flux.just(convertInternalToApiDetailedResponse(error)));
            }
        });
    }
    
    /**
     * Lists available operations.
     */
    @Override
    public Mono<ResponseEntity<ListOperations200Response>> listOperations(ServerWebExchange exchange) {
        ListOperations200Response response = new ListOperations200Response();
        
        Map<String, com.example.mcpstreaming.api.model.OperationInfo> operations = Map.of(
            "list_directory", createOperationInfo("Lists files and directories in a path", 
                Map.of("path", "string - directory path to list"), true),
            "read_file", createOperationInfo("Reads content of a file", 
                Map.of("path", "string - file path to read"), true),
            "create_file", createOperationInfo("Creates a new file with content", 
                Map.of("path", "string - file path to create", "content", "string - file content"), false),
            "edit_file", createOperationInfo("Edits (overwrites) a file with new content", 
                Map.of("path", "string - file path to edit", "content", "string - new file content"), false),
            "append_file", createOperationInfo("Appends content to an existing file", 
                Map.of("path", "string - file path to append to", "content", "string - content to append"), false),
            "grep", createOperationInfo("Searches for patterns in files", 
                Map.of("pattern", "string - regex pattern to search for", "path", "string - file or directory path to search", 
                       "recursive", "boolean - search recursively (default: false)", "case_sensitive", "boolean - case sensitive search (default: true)"), true),
            "execute_command", createOperationInfo("Executes a system command", 
                Map.of("command", "string - command to execute", "working_directory", "string - working directory (optional)", 
                       "timeout_seconds", "integer - timeout in seconds (optional)"), true)
        );
        
        response.setOperations(operations);
        
        return Mono.just(ResponseEntity.ok(response));
    }
    
    /**
     * Health check endpoint - now properly integrated with OpenAPI specification
     */
    @Override
    public Mono<ResponseEntity<HealthResponse>> health(ServerWebExchange exchange) {
        HealthResponse healthResponse = new HealthResponse();
        healthResponse.setStatus("UP");
        healthResponse.setService("MCP Streaming Service");
        healthResponse.setVersion("1.0.0");
        return Mono.just(ResponseEntity.ok(healthResponse));
    }
    
    /**
     * Handles non-streaming MCP requests.
     */
    private Mono<McpMessage> handleNonStreamingRequest(McpRequest request) {
        return switch (request.getOperation()) {
            case "list_directory" -> {
                String path = getStringParameter(request, "path", ".");
                yield fileOperationService.listDirectory(path)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "read_file" -> {
                String path = getStringParameter(request, "path");
                yield fileOperationService.readFile(path)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "create_file" -> {
                String path = getStringParameter(request, "path");
                String content = getStringParameter(request, "content", "");
                yield fileOperationService.createFile(path, content)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "edit_file" -> {
                String path = getStringParameter(request, "path");
                String content = getStringParameter(request, "content");
                yield fileOperationService.editFile(path, content)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "append_file" -> {
                String path = getStringParameter(request, "path");
                String content = getStringParameter(request, "content");
                yield fileOperationService.appendToFile(path, content)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "grep" -> {
                String pattern = getStringParameter(request, "pattern");
                String path = getStringParameter(request, "path", ".");
                boolean recursive = getBooleanParameter(request, "recursive", false);
                boolean caseSensitive = getBooleanParameter(request, "case_sensitive", true);
                yield grepService.grep(pattern, path, recursive, caseSensitive)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            case "execute_command" -> {
                String command = getStringParameter(request, "command");
                String workingDir = getStringParameter(request, "working_directory", null);
                int timeoutSeconds = getIntParameter(request, "timeout_seconds", 300);
                Duration timeout = Duration.ofSeconds(timeoutSeconds);
                yield commandExecutionService.executeCommand(command, workingDir, timeout)
                    .map(result -> new McpResponse(request.getId(), result))
                    .cast(McpMessage.class);
            }
            default -> Mono.just(new McpError(
                request.getId(),
                "UNKNOWN_OPERATION",
                "Unknown operation: " + request.getOperation()
            ));
        };
    }
    
    /**
     * Handles streaming MCP requests.
     */
    private Flux<McpMessage> handleStreamingOperation(McpRequest request) {
        AtomicLong sequenceNumber = new AtomicLong(0);
        
        return switch (request.getOperation()) {
            case "list_directory" -> {
                String path = getStringParameter(request, "path", ".");
                yield fileOperationService.listDirectoryStream(path)
                    .map(fileInfo -> new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        fileInfo
                    ))
                    .cast(McpMessage.class)
                    .concatWith(Flux.just(new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        "STREAM_COMPLETE",
                        true
                    )));
            }
            case "read_file" -> {
                String path = getStringParameter(request, "path");
                yield fileOperationService.readFileStream(path)
                    .map(chunk -> new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        chunk
                    ))
                    .cast(McpMessage.class)
                    .concatWith(Flux.just(new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        "STREAM_COMPLETE",
                        true
                    )));
            }
            case "grep" -> {
                String pattern = getStringParameter(request, "pattern");
                String path = getStringParameter(request, "path", ".");
                boolean recursive = getBooleanParameter(request, "recursive", false);
                boolean caseSensitive = getBooleanParameter(request, "case_sensitive", true);
                yield grepService.grepStream(pattern, path, recursive, caseSensitive)
                    .map(match -> new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        match
                    ))
                    .cast(McpMessage.class)
                    .concatWith(Flux.just(new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        "STREAM_COMPLETE",
                        true
                    )));
            }
            case "execute_command" -> {
                String command = getStringParameter(request, "command");
                String workingDir = getStringParameter(request, "working_directory", null);
                boolean includeStderr = getBooleanParameter(request, "include_stderr", true);
                yield commandExecutionService.executeCommandStream(command, workingDir, includeStderr)
                    .map(output -> new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        output
                    ))
                    .cast(McpMessage.class)
                    .concatWith(Flux.just(new McpStreamChunk(
                        request.getId(),
                        sequenceNumber.incrementAndGet(),
                        "STREAM_COMPLETE",
                        true
                    )));
            }
            default -> Flux.just(new McpError(
                request.getId(),
                "UNKNOWN_OPERATION",
                "Unknown streaming operation: " + request.getOperation()
            ));
        };
    }
    
    // Utility methods for parameter extraction
    private String getStringParameter(McpRequest request, String name) {
        Map<String, Object> params = request.getParameters();
        if (params == null || !params.containsKey(name)) {
            throw new RuntimeException("Missing required parameter: " + name);
        }
        return params.get(name).toString();
    }
    
    private String getStringParameter(McpRequest request, String name, String defaultValue) {
        Map<String, Object> params = request.getParameters();
        if (params == null || !params.containsKey(name)) {
            return defaultValue;  // Return the default value (which could be null)
        }
        return params.get(name).toString();
    }
    
    private boolean getBooleanParameter(McpRequest request, String name, boolean defaultValue) {
        Map<String, Object> params = request.getParameters();
        if (params == null || !params.containsKey(name)) {
            return defaultValue;
        }
        Object value = params.get(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    private int getIntParameter(McpRequest request, String name, int defaultValue) {
        Map<String, Object> params = request.getParameters();
        if (params == null || !params.containsKey(name)) {
            return defaultValue;
        }
        Object value = params.get(name);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    // Conversion methods between API models and internal models
    private McpRequest convertApiRequestToInternal(com.example.mcpstreaming.api.model.McpRequest apiRequest) {
        McpRequest internal = new McpRequest();
        internal.setId(apiRequest.getId());
        internal.setOperation(apiRequest.getOperation());
        internal.setParameters(apiRequest.getParameters());
        internal.setStream(apiRequest.getStream());
        return internal;
    }
    
    // Since the generated API models don't use inheritance, we need to always return McpMessage
    // with the appropriate type field to satisfy the interface contract
    private com.example.mcpstreaming.api.model.McpMessage convertInternalToApiMessage(McpMessage internal) {
        com.example.mcpstreaming.api.model.McpMessage apiMsg = new com.example.mcpstreaming.api.model.McpMessage();
        apiMsg.setId(internal.getId());
        apiMsg.setTimestamp(internal.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
        
        // Set appropriate type based on internal message type
        if (internal instanceof McpResponse) {
            apiMsg.setType("response");
        } else if (internal instanceof McpStreamChunk) {
            apiMsg.setType("stream_chunk");
        } else if (internal instanceof McpError) {
            apiMsg.setType("error");
        } else {
            apiMsg.setType("message");
        }
        
        return apiMsg;
    }
    
    // Method to convert and preserve detailed response information (for dedicated endpoints)
    private Object convertInternalToApiDetailedResponse(McpMessage internal) {
        if (internal instanceof McpResponse) {
            McpResponse resp = (McpResponse) internal;
            com.example.mcpstreaming.api.model.McpResponse apiResp = new com.example.mcpstreaming.api.model.McpResponse();
            apiResp.setId(resp.getId());
            apiResp.setRequestId(resp.getRequestId());
            apiResp.setStatus(resp.getStatus());
            apiResp.setResult(resp.getResult());
            apiResp.setStreamComplete(resp.isStreamComplete());
            apiResp.setTimestamp(resp.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
            apiResp.setType("response");
            return apiResp;
        } else if (internal instanceof McpStreamChunk) {
            McpStreamChunk chunk = (McpStreamChunk) internal;
            com.example.mcpstreaming.api.model.McpStreamChunk apiChunk = new com.example.mcpstreaming.api.model.McpStreamChunk();
            apiChunk.setId(chunk.getId());
            apiChunk.setRequestId(chunk.getRequestId());
            apiChunk.setSequenceNumber(chunk.getSequence());
            apiChunk.setData(chunk.getData());
            apiChunk.setIsFinal(chunk.isFinal());
            apiChunk.setTimestamp(chunk.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
            apiChunk.setType("stream_chunk");
            return apiChunk;
        } else if (internal instanceof McpError) {
            McpError error = (McpError) internal;
            com.example.mcpstreaming.api.model.McpError apiError = new com.example.mcpstreaming.api.model.McpError();
            apiError.setId(error.getId());
            apiError.setRequestId(error.getRequestId());
            apiError.setErrorCode(error.getErrorCode());
            apiError.setErrorMessage(error.getErrorMessage());
            apiError.setErrorType(error.getDetails() != null ? error.getDetails().toString() : "UNKNOWN");
            apiError.setTimestamp(error.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
            apiError.setType("error");
            return apiError;
        } else {
            // Generic message conversion
            com.example.mcpstreaming.api.model.McpMessage apiMsg = new com.example.mcpstreaming.api.model.McpMessage();
            apiMsg.setId(internal.getId());
            apiMsg.setType("message");  // Default type
            apiMsg.setTimestamp(internal.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
            return apiMsg;
        }
    }
    
    private com.example.mcpstreaming.api.model.McpMessage convertInternalToApiMessageBase(McpMessage internal) {
        com.example.mcpstreaming.api.model.McpMessage apiMsg = new com.example.mcpstreaming.api.model.McpMessage();
        apiMsg.setId(internal.getId());
        apiMsg.setTimestamp(internal.getTimestamp().atOffset(java.time.ZoneOffset.UTC));
        
        // Set appropriate type based on internal message type
        if (internal instanceof McpResponse) {
            apiMsg.setType("response");
        } else if (internal instanceof McpStreamChunk) {
            apiMsg.setType("stream_chunk");
        } else if (internal instanceof McpError) {
            apiMsg.setType("error");
        } else {
            apiMsg.setType("message");
        }
        
        return apiMsg;
    }
    
    private com.example.mcpstreaming.api.model.OperationInfo createOperationInfo(String description, Map<String, String> parameters, boolean streaming) {
        com.example.mcpstreaming.api.model.OperationInfo operationInfo = new com.example.mcpstreaming.api.model.OperationInfo();
        operationInfo.setDescription(description);
        operationInfo.setParameters(parameters);
        operationInfo.setStreaming(streaming);
        return operationInfo;
    }
    
    // Backward compatibility methods for WebSocket handler
    public Mono<McpMessage> handleRequest(McpRequest request) {
        com.example.mcpstreaming.api.model.McpRequest apiRequest = new com.example.mcpstreaming.api.model.McpRequest();
        apiRequest.setOperation(request.getOperation());
        apiRequest.setParameters(request.getParameters());
        apiRequest.setStream(request.isStream());
        
        return handleRequest(Mono.just(apiRequest), null)
            .map(ResponseEntity::getBody)
            .map(this::convertApiObjectToInternalMessage);
    }
    
    public Flux<McpMessage> handleStreamingRequest(McpRequest request) {
        com.example.mcpstreaming.api.model.McpRequest apiRequest = new com.example.mcpstreaming.api.model.McpRequest();
        apiRequest.setOperation(request.getOperation());
        apiRequest.setParameters(request.getParameters());
        apiRequest.setStream(request.isStream());
        
        return handleStreamingRequest(Mono.just(apiRequest), null)
            .flatMapMany(response -> response.getBody())
            .map(this::convertApiObjectToInternalMessage);
    }
    
    private McpMessage convertApiObjectToInternalMessage(Object apiObject) {
        if (apiObject instanceof com.example.mcpstreaming.api.model.McpResponse) {
            com.example.mcpstreaming.api.model.McpResponse apiResp = (com.example.mcpstreaming.api.model.McpResponse) apiObject;
            McpResponse internal = new McpResponse();
            internal.setId(apiResp.getId());
            internal.setRequestId(apiResp.getRequestId());
            internal.setStatus(apiResp.getStatus());
            internal.setResult(apiResp.getResult());
            internal.setStreamComplete(apiResp.getStreamComplete());
            return internal;
        } else if (apiObject instanceof com.example.mcpstreaming.api.model.McpStreamChunk) {
            com.example.mcpstreaming.api.model.McpStreamChunk apiChunk = (com.example.mcpstreaming.api.model.McpStreamChunk) apiObject;
            McpStreamChunk internal = new McpStreamChunk();
            internal.setId(apiChunk.getId());
            internal.setRequestId(apiChunk.getRequestId());
            internal.setSequence(apiChunk.getSequenceNumber());
            internal.setData(apiChunk.getData());
            internal.setFinal(apiChunk.getIsFinal());
            return internal;
        } else if (apiObject instanceof com.example.mcpstreaming.api.model.McpError) {
            com.example.mcpstreaming.api.model.McpError apiError = (com.example.mcpstreaming.api.model.McpError) apiObject;
            McpError internal = new McpError();
            internal.setId(apiError.getId());
            internal.setRequestId(apiError.getRequestId());
            internal.setErrorCode(apiError.getErrorCode());
            internal.setErrorMessage(apiError.getErrorMessage());
            return internal;
        } else if (apiObject instanceof com.example.mcpstreaming.api.model.McpMessage) {
            com.example.mcpstreaming.api.model.McpMessage apiMessage = (com.example.mcpstreaming.api.model.McpMessage) apiObject;
            // Convert basic McpMessage to McpResponse for backward compatibility
            McpResponse internal = new McpResponse();
            internal.setId(apiMessage.getId());
            internal.setStatus("success");
            return internal;
        } else {
            // Return a generic McpResponse for unknown types
            McpResponse internal = new McpResponse();
            internal.setStatus("success");
            return internal;
        }
    }
    
    private McpMessage convertApiToInternalMessage(com.example.mcpstreaming.api.model.McpMessage apiMessage) {
        // Since API models don't use inheritance, we need to determine type by the type field
        String messageType = apiMessage.getType();
        
        if ("response".equals(messageType)) {
            McpResponse internal = new McpResponse();
            internal.setId(apiMessage.getId());
            // Set basic response fields - more complex conversion would need the actual response object
            internal.setStatus("success");
            return internal;
        } else if ("stream_chunk".equals(messageType)) {
            McpStreamChunk internal = new McpStreamChunk();
            internal.setId(apiMessage.getId());
            return internal;
        } else if ("error".equals(messageType)) {
            McpError internal = new McpError();
            internal.setId(apiMessage.getId());
            return internal;
        } else {
            // Return a generic McpResponse for unknown types
            McpResponse internal = new McpResponse();
            internal.setId(apiMessage.getId());
            return internal;
        }
    }
}
