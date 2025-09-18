#!/bin/bash

# MCP Test Client Quick Start Script

set -e

echo "=== MCP Test Client ==="
echo ""

# Check if service is running
echo "Checking if MCP service is running..."
if ! curl -s http://localhost:8080/api/mcp/health > /dev/null; then
    echo "❌ MCP service is not running."
    echo "Please start the service first with: ./start-service.sh"
    exit 1
fi
echo "✅ MCP service is running"

# Build if needed
echo ""
echo "Building client..."
mvn compile -q
echo "✅ Client built"

echo ""
echo "Starting MCP Test Client..."

# Check for command line arguments
if [ $# -eq 0 ]; then
    echo "Starting in interactive mode..."
    echo "Type 'help' for available commands, 'quit' to exit"
    echo ""
    mvn exec:java -Dexec.mainClass="com.example.mcpstreaming.client.McpTestClient" -Dexec.args="-i" -q
elif [ "$1" = "demo" ]; then
    echo "Running demonstration mode..."
    echo ""
    mvn exec:java -Dexec.mainClass="com.example.mcpstreaming.client.McpTestClient" -Dexec.args="-d" -q
else
    echo "Running with custom arguments: $*"
    echo ""
    mvn exec:java -Dexec.mainClass="com.example.mcpstreaming.client.McpTestClient" -Dexec.args="$*" -q
fi