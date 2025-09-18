#!/bin/bash

# Start Llama Chat Service
# This script starts the chat service on port 8081

echo "Starting Llama Chat Service..."
echo "Service will be available at: http://localhost:8081"
echo "API endpoints will be at: http://localhost:8081/api/chat"
echo ""
echo "Make sure the following are running:"
echo "1. Ollama service (http://localhost:11434)"
echo "2. MCP Streaming Service (http://localhost:8080)"
echo ""

# Check Java version
java -version
if [ $? -ne 0 ]; then
    echo "Error: Java 21+ is required but not found"
    exit 1
fi

# Build and run
mvn clean compile exec:java -Dexec.mainClass="com.example.chatservice.LlamaChatServiceApplication"