#!/bin/bash

# Start All Services Script
# This script starts both the MCP Streaming Service and the Llama Chat Service

echo "=== Starting MCP Streaming + Llama Chat Services ==="
echo ""
echo "This will start:"
echo "1. MCP Streaming Service on port 8080"
echo "2. Llama Chat Service on port 8081"
echo ""
echo "Prerequisites:"
echo "- Java 21+ installed"
echo "- Ollama running on http://localhost:11434"
echo "- Maven installed"
echo ""

# Check Java version
echo "Checking Java version..."
java -version
if [ $? -ne 0 ]; then
    echo "Error: Java 21+ is required but not found"
    exit 1
fi

# Check Maven
echo "Checking Maven..."
mvn -version
if [ $? -ne 0 ]; then
    echo "Error: Maven is required but not found"
    exit 1
fi

# Check Ollama
echo "Checking Ollama availability..."
curl -s http://localhost:11434/api/tags > /dev/null
if [ $? -ne 0 ]; then
    echo "Warning: Ollama service not detected at http://localhost:11434"
    echo "Please make sure Ollama is running before using the chat service"
    echo ""
fi

echo "Starting services..."
echo ""

# Function to cleanup background processes on exit
cleanup() {
    echo ""
    echo "Shutting down services..."
    kill $MCP_PID $CHAT_PID 2>/dev/null
    wait
    echo "Services stopped"
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM EXIT

# Start MCP Streaming Service
echo "=== Starting MCP Streaming Service ==="
cd /Users/chris/code/warp_experiments/mindmap/java_mcp_streaming
mvn spring-boot:run &
MCP_PID=$!
echo "MCP Streaming Service starting with PID: $MCP_PID"
echo "Will be available at: http://localhost:8080"
echo ""

# Wait a bit for MCP service to start
echo "Waiting 15 seconds for MCP service to start..."
sleep 15

# Start Chat Service
echo "=== Starting Llama Chat Service ==="
cd /Users/chris/code/warp_experiments/mindmap/java_mcp_streaming/chat-service
mvn spring-boot:run &
CHAT_PID=$!
echo "Chat Service starting with PID: $CHAT_PID"
echo "Will be available at: http://localhost:8081"
echo ""

echo "=== Services Starting ==="
echo "MCP Streaming Service: http://localhost:8080"
echo "Llama Chat Service: http://localhost:8081"
echo ""
echo "Health checks:"
echo "- MCP Health: http://localhost:8080/api/mcp/health"
echo "- Chat Health: http://localhost:8081/api/chat/health"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Wait for both services
wait