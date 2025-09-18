package com.example.mcpstreaming.websocket;

import com.example.mcpstreaming.controller.McpStreamingController;
import com.example.mcpstreaming.model.McpError;
import com.example.mcpstreaming.model.McpMessage;
import com.example.mcpstreaming.model.McpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WebSocket handler for real-time MCP streaming communication.
 */
@Component
public class McpWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(McpWebSocketHandler.class);
    
    @Autowired
    private McpStreamingController mcpController;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        logger.info("WebSocket session established: {}", session.getId());
        
        return session.receive()
            .map(webSocketMessage -> webSocketMessage.getPayloadAsText())
            .doOnNext(message -> logger.debug("Received WebSocket message: {}", message))
            .flatMap(this::parseMessage)
            .flatMap(request -> handleMcpRequest(request, session))
            .doOnError(error -> logger.error("WebSocket error", error))
            .doOnComplete(() -> logger.info("WebSocket session completed: {}", session.getId()))
            .then();
    }
    
    /**
     * Parses incoming JSON message into MCP request.
     */
    private Mono<McpRequest> parseMessage(String message) {
        try {
            McpRequest request = objectMapper.readValue(message, McpRequest.class);
            return Mono.just(request);
        } catch (Exception e) {
            logger.error("Error parsing MCP request", e);
            return Mono.error(new RuntimeException("Invalid JSON message: " + e.getMessage()));
        }
    }
    
    /**
     * Handles MCP request and sends response(s) through WebSocket.
     */
    private Mono<Void> handleMcpRequest(McpRequest request, WebSocketSession session) {
        logger.info("Handling WebSocket MCP request: operation={}", request.getOperation());
        
        try {
            if (request.isStream()) {
                // Handle streaming request
                return handleStreamingRequest(request, session);
            } else {
                // Handle single request
                return handleSingleRequest(request, session);
            }
        } catch (Exception e) {
            logger.error("Error handling MCP request", e);
            McpError error = new McpError(
                request.getId(),
                "REQUEST_ERROR",
                "Error processing request: " + e.getMessage()
            );
            return sendMessage(session, error);
        }
    }
    
    /**
     * Handles single (non-streaming) MCP request.
     */
    private Mono<Void> handleSingleRequest(McpRequest request, WebSocketSession session) {
        // Delegate to the controller's logic
        return mcpController.handleRequest(request)
            .flatMap(response -> sendMessage(session, response))
            .doOnError(error -> {
                logger.error("Error in single request handling", error);
                McpError errorResponse = new McpError(
                    request.getId(),
                    "EXECUTION_ERROR",
                    "Error executing request: " + error.getMessage()
                );
                sendMessage(session, errorResponse).subscribe();
            });
    }
    
    /**
     * Handles streaming MCP request.
     */
    private Mono<Void> handleStreamingRequest(McpRequest request, WebSocketSession session) {
        // Create a streaming flux and send each message
        Flux<McpMessage> messageStream = mcpController.handleStreamingRequest(request);
        
        return messageStream
            .flatMap(message -> sendMessage(session, message))
            .doOnError(error -> {
                logger.error("Error in streaming request handling", error);
                McpError errorResponse = new McpError(
                    request.getId(),
                    "STREAM_ERROR",
                    "Error in stream: " + error.getMessage()
                );
                sendMessage(session, errorResponse).subscribe();
            })
            .then();
    }
    
    /**
     * Sends a message through the WebSocket session.
     */
    private Mono<Void> sendMessage(WebSocketSession session, McpMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            logger.debug("Sending WebSocket message: {}", json);
            
            return session.send(Mono.just(session.textMessage(json)))
                .doOnError(error -> logger.error("Error sending WebSocket message", error));
        } catch (Exception e) {
            logger.error("Error serializing message to JSON", e);
            return Mono.error(e);
        }
    }
}