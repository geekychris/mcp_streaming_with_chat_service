# MCP Streaming + Llama Chat Services

This project provides two complementary Spring Boot services that work together to create a powerful AI chat interface with tool calling capabilities:

1. **MCP Streaming Service** (Port 8080) - Provides secure OS access primitives
2. **Llama Chat Service** (Port 8081) - AI chat interface with tool integration

## Table of Contents

- [Quick Start](#quick-start)
- [Architecture Overview](#architecture-overview)
- [Services](#services)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Usage Examples](#usage-examples)
- [API Documentation](#api-documentation)
- [Troubleshooting](#troubleshooting)

## Quick Start

### 1. Install Prerequisites

```bash
# Install Java 21+ (using SDKMAN)
curl -s "https://get.sdkman.io" | bash
sdk install java 21.0.6-amzn

# Install Ollama (macOS)
brew install ollama

# Start Ollama and pull a model
ollama serve
ollama pull llama3.2:latest
```

### 2. Start All Services

```bash
# Start both MCP and Chat services
./start-all-services.sh
```

### 3. Test the Setup

```bash
# Test MCP service
curl http://localhost:8080/api/mcp/health

# Test Chat service
curl http://localhost:8081/api/chat/health

# Send a chat message with tool calling
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "List the files in the current directory and tell me what you see"}'
```

## Architecture Overview

```
┌─────────────────┐    HTTP     ┌─────────────────┐    HTTP     ┌─────────────────┐
│   Client App    │ ──────────► │  Llama Chat     │ ──────────► │  MCP Streaming  │
│                 │             │    Service      │             │     Service     │
│  - Web UI       │ ◄────────── │   (Port 8081)   │ ◄────────── │   (Port 8080)   │
│  - CLI Client   │    JSON     │                 │    JSON     │                 │
│  - API Client   │             │  - Chat Logic   │             │ - File Ops      │
└─────────────────┘             │  - Tool Calls   │             │ - Commands      │
                                │  - Conversation │             │ - Search        │
                                └─────────────────┘             └─────────────────┘
                                         │                               │
                                         │ HTTP                          │
                                         ▼                               ▼
                                ┌─────────────────┐             ┌─────────────────┐
                                │     Ollama      │             │   OS Services   │
                                │  (Port 11434)   │             │                 │
                                │                 │             │ - File System   │
                                │ - Llama Models  │             │ - Process Exec  │
                                │ - Inference     │             │ - Pattern Match │
                                │ - Tool Parsing  │             └─────────────────┘
                                └─────────────────┘
```

## Services

### MCP Streaming Service (Port 8080)

Provides secure access to operating system primitives through a REST API and WebSocket interface.

**Key Features:**
- File system operations (read, write, list, search)
- Command execution with safety validation
- Pattern matching and grep functionality
- Streaming support for large operations
- WebSocket real-time interface

**Endpoints:**
- `GET /api/mcp/health` - Health check
- `GET /api/mcp/operations` - List available operations
- `POST /api/mcp/request` - Execute single operation
- `POST /api/mcp/stream` - Execute with streaming
- `WS /ws/mcp` - WebSocket interface

### Llama Chat Service (Port 8081)

AI-powered chat interface that uses the MCP service as a tool library.

**Key Features:**
- Llama model integration via Ollama
- Automatic tool calling based on user requests
- Conversation history management
- Configurable model parameters
- Health monitoring and service discovery

**Endpoints:**
- `POST /api/chat/message` - Send chat message
- `GET /api/chat/conversation/{id}/history` - Get conversation history
- `GET /api/chat/health` - Health check
- `GET /api/chat/capabilities` - List models and tools

## Prerequisites

### Required Software

1. **Java 21+** (Amazon Corretto 23 JDK preferred)
2. **Maven 3.8+** for building
3. **Ollama** for Llama model inference
4. **curl** and **jq** for testing (optional)

### System Requirements

- **Memory**: 8GB+ RAM (for running Llama models)
- **Storage**: 10GB+ free space (for models and data)
- **OS**: macOS, Linux, or Windows with WSL2

## Installation & Setup

### 1. Clone and Build

```bash
# Navigate to project directory
cd /Users/chris/code/warp_experiments/mindmap/java_mcp_streaming

# Build MCP service
mvn clean compile

# Build Chat service
cd chat-service
mvn clean compile
cd ..
```

### 2. Setup Ollama

```bash
# Install Ollama (macOS)
brew install ollama

# Start Ollama service
ollama serve

# In another terminal, pull required models
ollama pull llama3.2:latest
ollama pull llama3.2:3b  # Optional: smaller model

# Verify installation
curl http://localhost:11434/api/tags
```

### 3. Start Services

#### Option 1: Start All at Once
```bash
./start-all-services.sh
```

#### Option 2: Start Individually
```bash
# Terminal 1: Start MCP service
./start-service.sh

# Terminal 2: Start Chat service
cd chat-service
./start-chat-service.sh
```

### 4. Verify Setup

```bash
# Check all services
curl http://localhost:8080/api/mcp/health    # MCP service
curl http://localhost:8081/api/chat/health   # Chat service
curl http://localhost:11434/api/tags         # Ollama
```

## Usage Examples

### Basic Chat

```bash
# Simple conversation
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello! How can you help me today?"}'
```

### File Operations via Chat

```bash
# Ask AI to list files
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "What files are in the current directory?"}'

# Ask AI to read a file
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Read the README.md file and summarize it"}'

# Ask AI to create and edit files
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Create a Python script that prints hello world, then run it"}'
```

### System Commands via Chat

```bash
# Ask AI to check system status
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me the current system processes and memory usage"}'

# Ask AI to find files
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Find all Java files in this project and tell me what they do"}'
```

### Advanced Conversation

```bash
# Multi-turn conversation with context
CONV_ID=$(curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Create a new directory called test-project"}' | jq -r '.conversation_id')

curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"Now create a simple Java hello world program in that directory\", \"conversation_id\": \"$CONV_ID\"}"

curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d "{\"message\": \"Compile and run the program you just created\", \"conversation_id\": \"$CONV_ID\"}"
```

## API Documentation

### MCP Streaming Service API

#### Execute Operation
```http
POST /api/mcp/request
Content-Type: application/json

{
  "operation": "list_directory",
  "parameters": {
    "path": "."
  }
}
```

#### Available Operations
- `list_directory`: List files and directories
- `read_file`: Read file contents
- `create_file`: Create new file
- `edit_file`: Edit existing file
- `append_file`: Append to file
- `execute_command`: Run system command
- `grep`: Search for patterns

### Chat Service API

#### Send Message
```http
POST /api/chat/message
Content-Type: application/json

{
  "message": "Your message here",
  "conversation_id": "optional-id",
  "model": "llama3.2:latest",
  "enable_tools": true,
  "temperature": 0.7,
  "max_tokens": 2000
}
```

#### Conversation Management
```http
GET /api/chat/conversation/{conversationId}/history
DELETE /api/chat/conversation/{conversationId}
GET /api/chat/conversations
```

## Configuration

### MCP Service Configuration

Edit `src/main/resources/application.yml`:

```yaml
mcp:
  streaming:
    max-concurrent-streams: 10
    default-timeout-seconds: 300
    security:
      validate-commands: true
      allow-dangerous-commands: false
```

### Chat Service Configuration

Edit `chat-service/src/main/resources/application.yml`:

```yaml
chat:
  ollama:
    base-url: http://localhost:11434
    default-model: llama3.2:latest
    temperature: 0.7
  mcp:
    service-url: http://localhost:8080
  tools:
    enabled: true
    max-calls-per-turn: 5
```

## Troubleshooting

### Common Issues

#### Services Won't Start

```bash
# Check Java version (must be 21+)
java -version

# Check ports are available
lsof -i :8080  # MCP service
lsof -i :8081  # Chat service

# Check Maven
mvn -version
```

#### Ollama Issues

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Restart Ollama
pkill ollama
ollama serve

# Check available models
ollama list
```

#### Tool Calls Not Working

1. Verify MCP service is running: `curl http://localhost:8080/api/mcp/health`
2. Check tool configuration: `enable_tools: true` in requests
3. Check MCP operations: `curl http://localhost:8080/api/mcp/operations`
4. Review logs for errors

#### Connection Issues

```bash
# Test service connectivity
curl -v http://localhost:8080/api/mcp/health
curl -v http://localhost:8081/api/chat/health

# Check service logs
tail -f mcp-service.log
tail -f chat-service.log
```

### Debug Mode

```bash
# Enable debug logging for MCP service
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.example.mcpstreaming=DEBUG"

# Enable debug logging for Chat service
cd chat-service
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.example.chatservice=DEBUG"
```

### Performance Optimization

For better performance with limited resources:

```yaml
# Use smaller models
chat:
  ollama:
    default-model: llama3.2:3b

# Reduce timeouts
chat:
  ollama:
    timeout-seconds: 60
  tools:
    max-calls-per-turn: 3
```

## Development

### Project Structure

```
java_mcp_streaming/
├── src/main/java/com/example/mcpstreaming/    # MCP Service
│   ├── controller/     # REST controllers
│   ├── service/        # Business logic
│   ├── model/          # Data models
│   └── websocket/      # WebSocket handlers
├── chat-service/       # Chat Service
│   └── src/main/java/com/example/chatservice/
│       ├── controller/ # REST controllers
│       ├── service/    # Business logic
│       └── model/      # Data models
├── start-all-services.sh    # Start both services
├── start-service.sh         # Start MCP service only
└── README.md               # This file
```

### Adding New Features

1. **New MCP Operations**: Add to `FileOperationService`, `CommandExecutionService`, etc.
2. **New Chat Features**: Extend `ChatService` and `ChatController`
3. **New Tools**: Update `OllamaService.createMcpTools()`

### Testing

```bash
# Run unit tests
mvn test
cd chat-service && mvn test

# Integration tests
./start-all-services.sh
# Run test scripts in another terminal
```

## License

This project demonstrates MCP streaming services with Llama chat integration. Use and modify as needed for your requirements.

---

**Support**: For issues or questions, check the individual service README files in their respective directories.