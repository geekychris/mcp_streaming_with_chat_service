# Llama Chat Service

A Spring Boot service that provides an intelligent chat interface using Llama models via Ollama, with integrated MCP (Model Context Protocol) tool calling capabilities. This service can execute file operations, system commands, and other tools through the companion MCP Streaming Service.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Tool Calling](#tool-calling)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## Overview

### Key Features

- **Llama Integration**: Uses Ollama for local LLM inference
- **Tool Calling**: Integrates with MCP Streaming Service for file operations, command execution, and more
- **Conversation Management**: Maintains conversation history and context
- **RESTful API**: Clean HTTP endpoints for integration
- **Reactive Programming**: Built with Spring WebFlux for non-blocking operations
- **Health Monitoring**: Built-in health checks and service monitoring

### Service Ports

- **Chat Service**: http://localhost:8081
- **MCP Streaming Service**: http://localhost:8080 (companion service)
- **Ollama**: http://localhost:11434 (external dependency)

## Architecture

```
┌─────────────────┐    HTTP     ┌─────────────────┐    HTTP     ┌─────────────────┐
│   Client App    │ ──────────► │  Llama Chat     │ ──────────► │  MCP Streaming  │
│                 │             │    Service      │             │     Service     │
│  - Web UI       │ ◄────────── │                 │ ◄────────── │                 │
│  - CLI Client   │    JSON     │  - Chat Logic   │    JSON     │ - File Ops      │
│  - API Client   │             │  - Tool Calls   │             │ - Commands      │
└─────────────────┘             │  - Conversation │             │ - Search        │
                                └─────────────────┘             └─────────────────┘
                                         │
                                         │ HTTP
                                         ▼
                                ┌─────────────────┐
                                │     Ollama      │
                                │                 │
                                │ - Llama Models  │
                                │ - Inference     │
                                │ - Tool Parsing  │
                                └─────────────────┘
```

### Component Overview

- **ChatController**: REST API endpoints for chat functionality
- **ChatService**: Main orchestration logic for chat processing
- **OllamaService**: Integration with Ollama for LLM inference
- **McpClientService**: Client for MCP Streaming Service tool calls
- **ConversationService**: In-memory conversation history management

## Prerequisites

### Required Software

1. **Java 21+** (OpenJDK or Amazon Corretto)
2. **Maven 3.8+** for building
3. **Ollama** running locally on port 11434
4. **MCP Streaming Service** running on port 8080

### Setting Up Ollama

```bash
# Install Ollama (macOS)
brew install ollama

# Start Ollama service
ollama serve

# Pull a Llama model (in another terminal)
ollama pull llama3.2:latest

# Verify Ollama is running
curl http://localhost:11434/api/tags
```

### Setting Up MCP Streaming Service

The MCP Streaming Service should already be available in the parent directory. Start it first:

```bash
cd ..
./start-service.sh
```

## Quick Start

### 1. Start All Services

The easiest way is to start everything at once:

```bash
# From the parent directory
./start-all-services.sh
```

### 2. Start Chat Service Only

If MCP Streaming Service is already running:

```bash
# From chat-service directory
./start-chat-service.sh
```

### 3. Manual Start

```bash
# Build the application
mvn clean compile

# Run the application
mvn spring-boot:run

# Or run with custom properties
mvn spring-boot:run -Dspring-boot.run.arguments="--chat.ollama.default-model=llama3.2:3b"
```

### 4. Quick Health Check

```bash
# Check if services are running
curl http://localhost:8081/api/chat/ping
curl http://localhost:8081/api/chat/health
```

## API Documentation

### Base URL
```
http://localhost:8081/api/chat
```

### Endpoints

#### Send Chat Message
```http
POST /api/chat/message
Content-Type: application/json

{
  "message": "Hello! Can you help me list the files in my current directory?",
  "conversation_id": "optional-conversation-id",
  "model": "llama3.2:latest",
  "enable_tools": true,
  "temperature": 0.7,
  "max_tokens": 2000
}
```

**Response:**
```json
{
  "message": {
    "id": "msg-123",
    "role": "assistant",
    "content": "I'll help you list the files in your current directory.",
    "timestamp": "2024-01-01T12:00:00Z",
    "conversation_id": "conv-456",
    "tool_call_results": [
      {
        "id": "tool-789",
        "tool_name": "list_directory",
        "success": true,
        "result": ["file1.txt", "file2.java", "directory1/"]
      }
    ]
  },
  "conversation_id": "conv-456",
  "model_used": "llama3.2:latest",
  "tool_calls_made": [...],
  "processing_time_ms": 1250,
  "timestamp": "2024-01-01T12:00:00Z"
}
```

#### Get Conversation History
```http
GET /api/chat/conversation/{conversationId}/history
```

#### Clear Conversation
```http
DELETE /api/chat/conversation/{conversationId}
```

#### Get Active Conversations
```http
GET /api/chat/conversations
```

#### Service Health
```http
GET /api/chat/health
```

#### Service Capabilities
```http
GET /api/chat/capabilities
```

#### Simple Ping
```http
GET /api/chat/ping
```

## Tool Calling

The chat service integrates with the MCP Streaming Service to provide the following tools:

### File System Operations
- **list_directory**: List files and directories
- **read_file**: Read file contents
- **create_file**: Create new files
- **edit_file**: Edit existing files
- **append_file**: Append to files

### System Operations
- **execute_command**: Run system commands
- **grep**: Search for patterns in files

### Example Tool Usage

```bash
# Send a message that will trigger tool calls
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Can you create a file called hello.txt with the content '\''Hello, World!'\'' and then read it back to me?"
  }'
```

The AI will automatically:
1. Use `create_file` tool to create the file
2. Use `read_file` tool to read the content back
3. Provide a natural language response with the results

## Configuration

### Application Properties

Edit `src/main/resources/application.yml`:

```yaml
chat:
  ollama:
    base-url: http://localhost:11434
    default-model: llama3.2:latest
    timeout-seconds: 120
    temperature: 0.7
    max-tokens: 2000
  
  mcp:
    service-url: http://localhost:8080
    timeout-seconds: 30
    max-retries: 3
    retry-delay-seconds: 1
  
  tools:
    enabled: true
    max-calls-per-turn: 5
    timeout-seconds: 60
```

### Environment Variables

```bash
# Override Ollama settings
export CHAT_OLLAMA_BASE_URL=http://localhost:11434
export CHAT_OLLAMA_DEFAULT_MODEL=llama3.2:3b

# Override MCP settings
export CHAT_MCP_SERVICE_URL=http://localhost:8080

# Server port
export SERVER_PORT=8081
```

## Usage Examples

### Basic Chat

```bash
# Simple conversation
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello! How are you today?"
  }'
```

### File Operations

```bash
# Ask AI to work with files
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Please read the README.md file and summarize its contents."
  }'
```

### System Commands

```bash
# Ask AI to run commands
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What processes are running on this system? Show me the top 5."
  }'
```

### Conversation Management

```bash
# Start a conversation
CONV_ID=$(curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello!"}' | jq -r '.conversation_id')

# Continue the conversation
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d "{
    \"message\": \"What did I just say?\",
    \"conversation_id\": \"$CONV_ID\"
  }"

# Get conversation history
curl http://localhost:8081/api/chat/conversation/$CONV_ID/history
```

### Advanced Configuration

```bash
# Use specific model with custom parameters
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Write a Python script to calculate fibonacci numbers",
    "model": "llama3.2:3b",
    "temperature": 0.2,
    "max_tokens": 1000,
    "enable_tools": true
  }'
```

## Testing

### Manual Testing

```bash
# Test service health
curl http://localhost:8081/api/chat/health

# Test basic functionality
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, world!"}'

# Test tool calling
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "List the files in the current directory"}'
```

### Running Unit Tests

```bash
mvn test
```

### Integration Testing

```bash
# Test with both services running
./start-all-services.sh

# In another terminal, run tests
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Create a test file, write some content to it, then read it back"}'
```

## Troubleshooting

### Common Issues

#### Chat Service Won't Start

```bash
# Check Java version
java -version

# Check if port 8081 is available
lsof -i :8081

# Check logs
mvn spring-boot:run | tee chat-service.log
```

#### Ollama Connection Issues

```bash
# Check Ollama service
curl http://localhost:11434/api/tags

# Check available models
ollama list

# Pull required model if missing
ollama pull llama3.2:latest
```

#### MCP Service Connection Issues

```bash
# Check MCP service
curl http://localhost:8080/api/mcp/health

# Check MCP operations
curl http://localhost:8080/api/mcp/operations
```

#### Tool Calls Not Working

1. Verify MCP service is running
2. Check tool configuration in application.yml
3. Ensure `enable_tools: true` in requests
4. Check logs for tool call errors

### Debug Mode

```bash
# Enable debug logging
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.example.chatservice=DEBUG"
```

### Health Monitoring

```bash
# Comprehensive health check
curl http://localhost:8081/api/chat/health | jq .

# Check service capabilities
curl http://localhost:8081/api/chat/capabilities | jq .

# Monitor active conversations
curl http://localhost:8081/api/chat/conversations | jq .
```

## Performance Tuning

### For High Load

```yaml
chat:
  ollama:
    timeout-seconds: 60  # Reduce timeout
  tools:
    max-calls-per-turn: 3  # Limit tool calls
  mcp:
    max-retries: 1  # Reduce retries
```

### For Better Responses

```yaml
chat:
  ollama:
    temperature: 0.3  # More focused responses
    max-tokens: 4000  # Longer responses
```

## Development

### Adding New Tool Types

1. Extend `OllamaService.createMcpTools()` to add new tool definitions
2. Update MCP Streaming Service to handle new operations
3. Test tool integration

### Extending the API

1. Add new endpoints to `ChatController`
2. Implement business logic in `ChatService`
3. Update documentation

## License

This project is provided as a demonstration of Llama chat integration with MCP tool calling. Use and modify as needed for your requirements.