#!/bin/bash

echo "Starting Llama Chat UI..."
echo "Frontend will be available at: http://localhost:3000"
echo "Make sure the Chat Service is running on http://localhost:8081"
echo ""

# Check if node and npm are available
if ! command -v node &> /dev/null; then
    echo "Error: Node.js is not installed"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "Error: npm is not installed"
    exit 1
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Check if chat service is running
echo "Checking if Chat Service is running..."
if curl -s http://localhost:8081/api/chat/ping > /dev/null 2>&1; then
    echo "✓ Chat Service is running"
else
    echo "⚠ Warning: Chat Service not detected at http://localhost:8081"
    echo "  Make sure to start the chat service first"
    echo ""
fi

# Start the development server
echo "Starting React development server..."
npm start