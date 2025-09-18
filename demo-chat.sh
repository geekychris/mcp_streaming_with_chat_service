#!/bin/bash

echo "=== Llama Chat Complete Demo ==="
echo ""
echo "This script demonstrates the complete chat system with UI."
echo ""

# Function to check if a service is running
check_service() {
    local url=$1
    local name=$2
    
    if curl -s "$url" > /dev/null 2>&1; then
        echo "✓ $name is running"
        return 0
    else
        echo "✗ $name is not running at $url"
        return 1
    fi
}

# Check all services
echo "Checking service status..."
echo ""

SERVICES_OK=true

# Check Ollama
if ! check_service "http://localhost:11434/api/tags" "Ollama"; then
    echo "  Please start Ollama: ollama serve"
    SERVICES_OK=false
fi

# Check MCP Service
if ! check_service "http://localhost:8080/api/mcp/health" "MCP Streaming Service"; then
    echo "  Please start MCP service: ./start-service.sh"
    SERVICES_OK=false
fi

# Check Chat Service
if ! check_service "http://localhost:8081/api/chat/ping" "Chat Service"; then
    echo "  Please start Chat service: cd chat-service && ./start-chat-service.sh"
    SERVICES_OK=false
fi

# Check React UI (optional)
if check_service "http://localhost:3000" "React UI"; then
    UI_RUNNING=true
else
    UI_RUNNING=false
    echo "  React UI not running - will show command line demo only"
fi

echo ""

if [ "$SERVICES_OK" = false ]; then
    echo "❌ Some services are not running. Please start them first."
    echo ""
    echo "Quick start all services:"
    echo "  ./start-complete-stack.sh"
    echo ""
    exit 1
fi

echo "✅ All backend services are running!"
echo ""

if [ "$UI_RUNNING" = true ]; then
    echo "🌐 React UI is available at: http://localhost:3000"
    echo ""
    echo "You can:"
    echo "1. Open http://localhost:3000 in your browser for the full UI experience"
    echo "2. Continue with this command-line demo"
    echo ""
    
    read -p "Press Enter to continue with command-line demo or Ctrl+C to exit..."
    echo ""
fi

echo "=== Command Line Demo ==="
echo ""
echo "Testing the chat service with various tool-enabled queries:"
echo ""

# Demo queries
QUERIES=(
    "Hello! Can you help me list the files in the current directory?"
    "Please read the README.md file and tell me what this project does."
    "Create a simple test file called demo.txt with some example content."
    "Can you show me the current system uptime?"
    "Search for any Java files in this project and tell me what you find."
)

for i in "${!QUERIES[@]}"; do
    query="${QUERIES[$i]}"
    echo "🤖 Demo Query $((i+1)): $query"
    echo ""
    echo "📤 Sending request..."
    
    response=$(curl -s -X POST http://localhost:8081/api/chat/message \
        -H "Content-Type: application/json" \
        -d "{\"message\": \"$query\", \"enable_tools\": true}")
    
    if [ $? -eq 0 ]; then
        echo "📥 Response received:"
        echo "$response" | jq -r '.message.content' 2>/dev/null || echo "$response"
        
        # Show tool calls if any
        tool_calls=$(echo "$response" | jq -r '.tool_calls_made[]?.tool_name' 2>/dev/null)
        if [ ! -z "$tool_calls" ] && [ "$tool_calls" != "null" ]; then
            echo ""
            echo "🔧 Tools used: $tool_calls"
        fi
        
        echo ""
        echo "⏱️  Processing time: $(echo "$response" | jq -r '.processing_time_ms // "unknown"')ms"
        
    else
        echo "❌ Failed to get response from chat service"
    fi
    
    echo ""
    echo "----------------------------------------"
    echo ""
    
    if [ $i -lt $((${#QUERIES[@]} - 1)) ]; then
        echo "Press Enter for next demo query..."
        read
        echo ""
    fi
done

echo "=== Demo Complete ==="
echo ""
echo "🎉 The chat system is working!"
echo ""

if [ "$UI_RUNNING" = true ]; then
    echo "💡 Try the web interface at: http://localhost:3000"
else
    echo "💡 Start the web interface with: cd chat-ui && ./start-ui.sh"
fi

echo ""
echo "Service URLs:"
echo "• MCP Service: http://localhost:8080/api/mcp/health"
echo "• Chat Service: http://localhost:8081/api/chat/health" 
echo "• React UI: http://localhost:3000"
echo ""
echo "Happy chatting! 🚀"