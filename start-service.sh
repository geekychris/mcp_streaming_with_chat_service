#!/bin/bash

# MCP Streaming Service Quick Start Script

set -e

echo "=== MCP Streaming Service Quick Start ==="
echo ""

# Check Java version
echo "Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 21 or later."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21 or later required. Found Java $JAVA_VERSION"
    exit 1
fi
echo "✅ Java $JAVA_VERSION found"

# Check Maven
echo "Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven 3.8+."
    exit 1
fi
echo "✅ Maven found"

# Build the application
echo ""
echo "Building application..."
mvn clean compile -q
echo "✅ Build completed"

# Run tests
echo ""
echo "Running tests..."
mvn test -q
echo "✅ Tests passed"

# Start the service
echo ""
echo "Starting MCP Streaming Service..."
echo "Service will be available at: http://localhost:8080"
echo "WebSocket endpoint: ws://localhost:8080/ws/mcp"
echo ""
echo "Press Ctrl+C to stop the service"
echo ""

# Start with Spring Boot Maven plugin
mvn spring-boot:run