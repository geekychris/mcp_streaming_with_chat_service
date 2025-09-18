#!/bin/bash

# Start Complete Stack Script
# This script starts MCP Service, Chat Service, and React UI

echo "=== Starting Complete Llama Chat Stack ==="
echo ""
echo "This will start:"
echo "1. MCP Streaming Service on port 8080"
echo "2. Llama Chat Service on port 8081" 
echo "3. React Chat UI on port 3000"
echo ""
echo "Prerequisites:"
echo "- Java 21+ installed"
echo "- Maven installed"
echo "- Node.js and npm installed"
echo "- Ollama running on http://localhost:11434"
echo ""

# Check prerequisites
echo "Checking prerequisites..."

# Check Java
java -version
if [ $? -ne 0 ]; then
    echo "Error: Java 21+ is required but not found"
    exit 1
fi

# Check Maven
mvn -version > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Error: Maven is required but not found"
    exit 1
fi

# Check Node.js
node -v > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Error: Node.js is required but not found"
    exit 1
fi

# Check npm
npm -v > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Error: npm is required but not found"
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

echo "Prerequisites check completed."
echo ""

# Function to cleanup background processes on exit
cleanup() {
    echo ""
    echo "Shutting down all services..."
    kill $MCP_PID $CHAT_PID $UI_PID 2>/dev/null
    wait
    echo "All services stopped"
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM EXIT

# Start MCP Streaming Service
echo "=== Starting MCP Streaming Service ==="
cd /Users/chris/code/warp_experiments/mindmap/java_mcp_streaming
mvn spring-boot:run > mcp-service.log 2>&1 &
MCP_PID=$!
echo "MCP Streaming Service starting with PID: $MCP_PID"
echo "Will be available at: http://localhost:8080"
echo "Logs: mcp-service.log"
echo ""

# Wait for MCP service to start
echo "Waiting 15 seconds for MCP service to start..."
sleep 15

# Start Chat Service
echo "=== Starting Llama Chat Service ==="
cd /Users/chris/code/warp_experiments/mindmap/java_mcp_streaming/chat-service
mvn spring-boot:run > ../chat-service.log 2>&1 &
CHAT_PID=$!
echo "Chat Service starting with PID: $CHAT_PID"
echo "Will be available at: http://localhost:8081"
echo "Logs: chat-service.log"
echo ""

# Wait for Chat service to start
echo "Waiting 15 seconds for Chat service to start..."
sleep 15

# Start React UI
echo "=== Starting React Chat UI ==="
cd /Users/chris/code/warp_experiments/mindmap/java_mcp_streaming/chat-ui

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing UI dependencies..."
    npm install
fi

npm start > ../ui.log 2>&1 &
UI_PID=$!
echo "React UI starting with PID: $UI_PID"
echo "Will be available at: http://localhost:3000"
echo "Logs: ui.log"
echo ""

echo "=== All Services Started ==="
echo "MCP Streaming Service: http://localhost:8080"
echo "Llama Chat Service: http://localhost:8081" 
echo "React Chat UI: http://localhost:3000"
echo ""
echo "Health checks:"
echo "- MCP Health: http://localhost:8080/api/mcp/health"
echo "- Chat Health: http://localhost:8081/api/chat/health"
echo "- UI: http://localhost:3000"
echo ""
echo "Log files:"
echo "- MCP Service: mcp-service.log"
echo "- Chat Service: chat-service.log" 
echo "- React UI: ui.log"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Wait for all services
wait