<p align="center">
  <img src="assets/logo.png" alt="MCP Streaming With Chat Service logo" width="180">
</p>

# MCP Streaming + Llama Chat with React UI

A comprehensive AI chat system that combines the power of Llama language models with Model Context Protocol (MCP) tool execution, featuring a modern React web interface. This project demonstrates how to build a complete AI assistant that can interact with the operating system, execute commands, manipulate files, and provide intelligent responses through a beautiful web interface.

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Components](#components)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Usage Examples](#usage-examples)
- [API Documentation](#api-documentation)
- [Development Guide](#development-guide)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🎯 Project Overview

### Purpose

This project addresses the challenge of creating an AI assistant that can not only engage in intelligent conversation but also take concrete actions on behalf of the user. By combining:

1. **Llama Language Models** for natural language understanding and generation
2. **Model Context Protocol (MCP)** for secure, structured tool execution
3. **Spring Boot Services** for robust backend architecture
4. **React Web Interface** for modern user experience

We create a complete AI assistant capable of:
- Understanding natural language requests
- Executing file system operations
- Running system commands safely
- Providing intelligent analysis and responses
- Maintaining conversation context and history

### Key Innovations

- **Tool-Aware AI**: The AI automatically determines when and how to use tools based on user requests
- **Secure Execution**: MCP provides a secure abstraction layer for system operations
- **Real-time Feedback**: Users see exactly what tools are being executed and their results
- **Conversation Persistence**: Full conversation history with context preservation
- **Service Architecture**: Modular design allows independent scaling and updates

## 🏗 Architecture

### High-Level System Architecture

```
┌─────────────────┐    HTTP/REST     ┌─────────────────┐    HTTP/REST    ┌─────────────────┐
│   React UI      │ ───────────────► │  Llama Chat     │ ──────────────► │  MCP Streaming  │
│  (Port 3000)    │                  │    Service      │                 │     Service     │
│                 │ ◄─────────────── │  (Port 8081)    │ ◄────────────── │  (Port 8080)    │
│  • Chat Feed    │     JSON         │                 │     JSON        │                 │
│  • Conversations│                  │  • Orchestration│                 │  • File Ops     │
│  • Tool Results │                  │  • LLM Calling  │                 │  • Commands     │
│  • Service Info │                  │  • Tool Parsing │                 │  • Pattern Match│
└─────────────────┘                  └─────────────────┘                 └─────────────────┘
         │                                    │                                    │
         │                                    │ HTTP                               │
         │                                    ▼                                    ▼
         │                           ┌─────────────────┐                  ┌─────────────────┐
         │                           │     Ollama      │                  │   OS Services   │
         │                           │  (Port 11434)   │                  │                 │
         │                           │                 │                  │  • File System  │
         └──── Browser Rendering ────│  • Llama Models │                  │  • Process Exec │
                                     │  • Inference    │                  │  • Search Ops   │
                                     │  • Tool Support │                  │  • Validation   │
                                     └─────────────────┘                  └─────────────────┘
```

### Data Flow Architecture

**Typical Chat Flow:**
1. **User Input** → React UI captures message
2. **API Request** → UI sends POST to Chat Service 
3. **Context Loading** → Chat Service loads conversation history
4. **LLM Generation** → Chat Service calls Ollama with tool definitions
5. **Tool Parsing** → If tools are called, Chat Service extracts tool calls
6. **Tool Execution** → Chat Service calls MCP Service for each tool
7. **System Operations** → MCP Service executes file/command operations
8. **Result Integration** → Chat Service integrates tool results 
9. **Final Generation** → Ollama generates final response with tool context
10. **Response Delivery** → UI displays message with tool execution details

## 🧩 Components

### 1. MCP Streaming Service (Port 8080)

**Purpose**: Provides secure, abstracted access to operating system primitives through a well-defined protocol.

**Architecture**:
```
src/main/java/com/example/mcpstreaming/
├── controller/
│   └── McpStreamingController.java     # REST API endpoints
├── service/
│   ├── FileOperationService.java      # File system operations
│   ├── CommandExecutionService.java   # System command execution
│   └── GrepService.java               # Pattern matching and search
├── model/
│   ├── McpRequest.java                # Request data models
│   ├── McpResponse.java               # Response data models
│   └── McpStreamChunk.java            # Streaming data chunks
├── websocket/
│   └── McpWebSocketHandler.java       # Real-time WebSocket interface
└── config/
    └── WebSocketConfig.java           # WebSocket configuration
```

**Key Features**:
- **Secure Operations**: Command validation, path sanitization, privilege controls
- **Streaming Support**: Large file operations with real-time progress
- **WebSocket Interface**: Real-time bidirectional communication
- **Comprehensive Operations**: Files, commands, search, pattern matching
- **Safety First**: Blacklisted dangerous commands, timeout enforcement

**Available Operations**:
- `list_directory` - List files and directories
- `read_file` - Read file contents with streaming support
- `create_file` - Create new files with content
- `edit_file` - Modify existing files
- `append_file` - Append content to files
- `execute_command` - Run system commands with validation
- `grep` - Search for patterns in files/directories

### 2. Llama Chat Service (Port 8081)

**Purpose**: Orchestrates conversation flow, integrates with Llama models, and manages tool calling logic.

**Architecture**:
```
chat-service/src/main/java/com/example/chatservice/
├── controller/
│   └── ChatController.java            # Chat API endpoints
├── service/
│   ├── ChatService.java              # Main conversation orchestration
│   ├── OllamaService.java            # Llama model integration
│   ├── McpClientService.java         # MCP service client
│   └── ConversationService.java      # Conversation history management
├── model/
│   ├── ChatMessage.java              # Chat message data model
│   ├── ChatRequest.java              # API request models
│   ├── ChatResponse.java             # API response models
│   ├── ToolCall.java                 # Tool calling data structures
│   ├── ToolCallResult.java           # Tool execution results
│   └── OllamaModels.java            # Ollama API integration models
└── config/
    └── application.yml               # Service configuration
```

**Key Features**:
- **Intelligent Tool Calling**: Automatically determines when and how to use tools
- **Conversation Management**: Maintains context across multiple turns
- **Multi-Model Support**: Works with various Llama models via Ollama
- **Error Recovery**: Graceful handling of tool failures and service interruptions
- **Performance Monitoring**: Request timing, tool usage analytics

**Processing Flow**:
1. **Receive User Message**: Parse and validate incoming chat requests
2. **Context Preparation**: Load conversation history and prepare context
3. **LLM Generation**: Send to Llama with available tool definitions
4. **Tool Execution**: If tools are called, execute via MCP service
5. **Result Integration**: Incorporate tool results into conversation
6. **Final Response**: Generate final response with complete context
7. **History Storage**: Save conversation for future context

### 3. React Chat UI (Port 3000)

**Purpose**: Provides a modern, responsive web interface for interacting with the chat system.

**Architecture**:
```
chat-ui/src/
├── components/
│   ├── App.tsx                       # Main application component
│   ├── ChatInterface.tsx             # Infinite scroll chat feed
│   ├── ChatMessage.tsx               # Individual message rendering
│   ├── MessageInput.tsx              # Smart input with auto-resize
│   └── Sidebar.tsx                   # Conversation and service management
├── hooks/
│   └── useChat.ts                    # Main state management hook
├── services/
│   └── chatApi.ts                    # Backend API client
├── types/
│   └── chat.ts                       # TypeScript type definitions
└── styles/
    └── App.css                       # Tailwind CSS configuration
```

**Key Features**:
- **Infinite Scroll Feed**: Smooth scrolling with auto-scroll behavior
- **Rich Message Rendering**: Markdown support with syntax highlighting
- **Tool Execution Visualization**: Real-time display of tool calls and results
- **Conversation Management**: Create, switch, and delete conversations
- **Service Monitoring**: Live health status of all backend services
- **Responsive Design**: Works seamlessly on desktop and mobile
- **Error Handling**: Graceful degradation and recovery mechanisms

**User Experience Flow**:
1. **Service Connection**: Automatically connects and monitors backend health
2. **Conversation Creation**: Users can start new conversations or continue existing ones
3. **Message Input**: Smart input field with keyboard shortcuts and auto-expand
4. **Real-time Feedback**: Immediate visual feedback for message processing
5. **Tool Visualization**: Clear display of which tools are being executed
6. **Result Integration**: Tool results are seamlessly integrated into conversation flow
7. **History Management**: Easy access to previous conversations and messages

### 4. Integration Layer: Ollama + System Services

**Ollama Integration**:
- **Local LLM Serving**: Runs Llama models locally for privacy and performance
- **Model Management**: Supports multiple model sizes and configurations
- **Tool Calling Protocol**: Structured function calling for reliable tool execution
- **Performance Optimization**: Optimized for local inference with reasonable hardware

**System Services**:
- **File System Access**: Secure, validated file operations
- **Command Execution**: Sandboxed system command execution
- **Pattern Matching**: Efficient search across files and directories
- **Process Management**: Safe process creation and monitoring

## 🚀 Quick Start

### Prerequisites

```bash
# 1. Java 21+ (Amazon Corretto recommended)
java -version

# 2. Maven 3.8+
mvn -version

# 3. Node.js 18+ and npm
node -v && npm -v

# 4. Ollama with Llama model
ollama --version
```

### One-Command Setup

```bash
# Install and start Ollama
brew install ollama
ollama serve  # In one terminal
ollama pull llama3.2:latest  # In another terminal

# Start complete system
git clone <repository>
cd java_mcp_streaming
./start-complete-stack.sh
```

### Access Points

- **React UI**: http://localhost:3000 (Main interface)
- **Chat Service**: http://localhost:8081 (API)
- **MCP Service**: http://localhost:8080 (Tools)
- **Ollama**: http://localhost:11434 (LLM)

## 🔧 Detailed Setup

### 1. Environment Setup

```bash
# Set Java version (if using SDKMAN)
sdk use java 21.0.6-amzn

# Set JAVA_HOME (example for Amazon Corretto)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-23.jdk/Contents/Home

# Verify Java configuration
java -version
echo $JAVA_HOME
```

### 2. Ollama Configuration

```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama service
ollama serve

# Pull required models
ollama pull llama3.2:latest      # Main model (4.3GB)
ollama pull llama3.2:3b          # Smaller model (2.0GB)

# Verify installation
curl http://localhost:11434/api/tags
```

### 3. Backend Services

```bash
# Build MCP Service
mvn clean compile

# Build Chat Service
cd chat-service
mvn clean compile
cd ..

# Start services individually
./start-service.sh              # MCP Service
cd chat-service && ./start-chat-service.sh  # Chat Service
```

### 4. Frontend Setup

```bash
cd chat-ui

# Install dependencies
npm install

# Start development server
npm start
# or use the custom script
./start-ui.sh
```

### 5. Configuration Options

**MCP Service** (`src/main/resources/application.yml`):
```yaml
mcp:
  streaming:
    max-concurrent-streams: 10
    default-timeout-seconds: 300
    security:
      validate-commands: true
      allow-dangerous-commands: false
```

**Chat Service** (`chat-service/src/main/resources/application.yml`):
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

**React UI** (`chat-ui/.env`):
```env
REACT_APP_API_URL=http://localhost:8081/api/chat
REACT_APP_ENV=development
```

## 💬 Usage Examples

### Web Interface Examples

1. **Open http://localhost:3000**
2. **Click "New Conversation"**
3. **Try these example queries:**

#### File System Operations
```
"List all files in the current directory"
→ Uses list_directory tool
→ Shows file listing in chat

"Read the README.md file and summarize it"
→ Uses read_file tool
→ AI provides intelligent summary

"Create a Python script that prints 'Hello World'"
→ Uses create_file tool
→ Creates actual file on system
```

#### System Administration
```
"Show me the current system uptime and memory usage"
→ Uses execute_command tool
→ Runs system commands safely

"Find all Java files in this project"
→ Uses grep tool
→ Searches and lists matching files

"Check if port 8080 is in use"
→ Uses execute_command with netstat/lsof
→ Shows port usage information
```

#### Code Analysis
```
"Analyze the structure of this Java project"
→ Uses multiple tools (list_directory, read_file, grep)
→ Provides comprehensive code analysis

"Find any TODO comments in the codebase"
→ Uses grep tool with pattern matching
→ Lists all TODO items found
```

### API Examples

**Direct Chat Service Usage:**
```bash
# Simple chat without tools
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello! How are you?"}'

# Chat with tools enabled
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "List the files in the current directory", "enable_tools": true}'

# Continue conversation
curl -X POST http://localhost:8081/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "Now read the first file", "conversation_id": "conv-123", "enable_tools": true}'
```

**Direct MCP Service Usage:**
```bash
# List directory via MCP
curl -X POST http://localhost:8080/api/mcp/request \
  -H "Content-Type: application/json" \
  -d '{"operation": "list_directory", "parameters": {"path": "."}}'

# Execute command via MCP
curl -X POST http://localhost:8080/api/mcp/request \
  -H "Content-Type: application/json" \
  -d '{"operation": "execute_command", "parameters": {"command": "uptime"}}'
```

## 📖 API Documentation

### Chat Service API

#### Send Message
```http
POST /api/chat/message
Content-Type: application/json

{
  "message": "Your message here",
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
    "content": "AI response here",
    "timestamp": "2024-01-01T12:00:00Z",
    "tool_call_results": [
      {
        "id": "tool-456",
        "tool_name": "list_directory", 
        "success": true,
        "result": ["file1.txt", "file2.java"]
      }
    ]
  },
  "conversation_id": "conv-789",
  "model_used": "llama3.2:latest",
  "processing_time_ms": 1250
}
```

#### Conversation Management
```http
# Get conversation history
GET /api/chat/conversation/{conversationId}/history

# Clear conversation
DELETE /api/chat/conversation/{conversationId}

# List active conversations
GET /api/chat/conversations

# Service health
GET /api/chat/health

# Service capabilities
GET /api/chat/capabilities
```

### MCP Service API

#### Execute Operation
```http
POST /api/mcp/request
Content-Type: application/json

{
  "operation": "operation_name",
  "parameters": {
    "param1": "value1",
    "param2": "value2"
  },
  "stream": false
}
```

#### Available Operations

| Operation | Parameters | Description |
|-----------|------------|-------------|
| `list_directory` | `path` | List files and directories |
| `read_file` | `path` | Read file contents |
| `create_file` | `path`, `content` | Create new file |
| `edit_file` | `path`, `content` | Edit existing file |
| `append_file` | `path`, `content` | Append to file |
| `execute_command` | `command`, `working_directory`, `timeout_seconds` | Run system command |
| `grep` | `pattern`, `path`, `recursive`, `case_sensitive` | Search for patterns |

#### Streaming Operations
```http
POST /api/mcp/stream
Content-Type: application/json
Accept: application/x-ndjson

{
  "operation": "read_file",
  "parameters": {"path": "/large/file.txt"},
  "stream": true
}
```

#### WebSocket Interface
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/mcp');

ws.onopen = () => {
  ws.send(JSON.stringify({
    operation: "list_directory",
    parameters: {path: "."}
  }));
};

ws.onmessage = (event) => {
  const response = JSON.parse(event.data);
  console.log('MCP Response:', response);
};
```

## 🛠 Development Guide

### Project Structure Overview

```
java_mcp_streaming/
├── src/main/java/com/example/mcpstreaming/  # MCP Streaming Service
│   ├── controller/                          # REST controllers
│   ├── service/                            # Business logic
│   ├── model/                              # Data models
│   ├── websocket/                          # WebSocket handlers
│   └── config/                             # Configuration
├── chat-service/                           # Llama Chat Service
│   └── src/main/java/com/example/chatservice/
│       ├── controller/                     # Chat API controllers
│       ├── service/                        # Chat business logic
│       ├── model/                          # Chat data models
│       └── config/                         # Chat configuration
├── chat-ui/                                # React UI
│   ├── src/
│   │   ├── components/                     # React components
│   │   ├── hooks/                          # Custom hooks
│   │   ├── services/                       # API clients
│   │   └── types/                          # TypeScript types
│   └── public/                             # Static assets
├── start-service.sh                        # Start MCP service
├── start-all-services.sh                   # Start backend services
├── start-complete-stack.sh                 # Start everything
├── demo-chat.sh                            # Demo script
└── README.md                               # This file
```

### Adding New Features

#### Adding New MCP Operations

1. **Define Operation Logic** (`service/CustomOperationService.java`):
```java
@Service
public class CustomOperationService {
    public Mono<CustomResult> performCustomOperation(String param) {
        // Implementation
    }
}
```

2. **Update Controller** (`controller/McpStreamingController.java`):
```java
case "custom_operation" -> {
    String param = getStringParameter(request, "param");
    yield customOperationService.performCustomOperation(param)
        .map(result -> new McpResponse(request.getId(), result));
}
```

3. **Add to Operations List**:
```java
operations.put("custom_operation", Map.of(
    "description", "Performs a custom operation",
    "parameters", Map.of("param", "string - parameter description"),
    "streaming", false
));
```

#### Adding New Chat Features

1. **Extend Chat Service** (`service/ChatService.java`):
```java
public Mono<CustomResponse> customChatFeature(CustomRequest request) {
    // Implementation
}
```

2. **Update Controller** (`controller/ChatController.java`):
```java
@PostMapping("/custom-feature")
public Mono<ResponseEntity<CustomResponse>> customFeature(@RequestBody CustomRequest request) {
    return chatService.customChatFeature(request)
        .map(ResponseEntity::ok);
}
```

3. **Update Frontend** (`services/chatApi.ts`):
```typescript
async customFeature(request: CustomRequest): Promise<CustomResponse> {
  return this.fetchWithErrorHandling(`${API_BASE_URL}/custom-feature`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
```

#### Adding New UI Components

1. **Create Component** (`components/CustomComponent.tsx`):
```typescript
interface CustomComponentProps {
  data: CustomData;
  onAction: (action: string) => void;
}

const CustomComponent: React.FC<CustomComponentProps> = ({ data, onAction }) => {
  return (
    <div className="custom-component">
      {/* Component implementation */}
    </div>
  );
};
```

2. **Update State Management** (`hooks/useChat.ts`):
```typescript
const [customState, setCustomState] = useState<CustomState>({});

const customAction = useCallback(async (param: string) => {
  // Custom action logic
}, []);

return {
  // ... existing state and actions
  customState,
  customAction,
};
```

## 🔧 Troubleshooting

### Common Issues and Solutions

#### Services Won't Start

**Java Version Issues:**
```bash
# Check Java version (must be 21+)
java -version

# If wrong version, install correct one
sdk install java 21.0.6-amzn
sdk use java 21.0.6-amzn

# Set JAVA_HOME
export JAVA_HOME=$(sdk home java 21.0.6-amzn)
```

**Port Conflicts:**
```bash
# Check what's using ports
lsof -i :8080  # MCP Service
lsof -i :8081  # Chat Service  
lsof -i :3000  # React UI
lsof -i :11434 # Ollama

# Kill processes if needed
kill -9 <PID>
```

#### Ollama Connection Issues

**Service Not Running:**
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama if not running
ollama serve

# Check available models
ollama list

# Pull models if missing
ollama pull llama3.2:latest
```

#### Frontend Issues

**UI Won't Load:**
```bash
# Check Node.js version
node -v  # Should be 16+

# Clear npm cache
npm cache clean --force

# Reinstall dependencies  
rm -rf node_modules package-lock.json
npm install

# Check for port conflicts
lsof -i :3000
```

### Debug Mode

#### Enable Debug Logging

**Backend Services:**
```bash
# MCP Service with debug
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.example.mcpstreaming=DEBUG"

# Chat Service with debug
cd chat-service
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.example.chatservice=DEBUG"
```

**Frontend:**
```bash
# React with debug info
REACT_APP_DEBUG=true npm start
```

#### Monitoring Tools

**Health Checks:**
```bash
# Comprehensive health check
curl http://localhost:8080/api/mcp/health | jq .
curl http://localhost:8081/api/chat/health | jq .
curl http://localhost:11434/api/tags | jq .

# Service capabilities
curl http://localhost:8081/api/chat/capabilities | jq .
```

## 🤝 Contributing

### Development Workflow

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Follow coding standards**: 
   - Java: Google Java Style
   - TypeScript: ESLint + Prettier
   - Commit messages: Conventional Commits
4. **Add tests** for new functionality
5. **Update documentation** as needed
6. **Submit a pull request**

### Testing Guidelines

**Write Tests For:**
- All public API endpoints
- Core business logic methods
- Error handling scenarios
- UI component interactions
- Integration between services

## 📄 License

This project is provided as a demonstration of integrating Llama language models with MCP tool execution and modern web interfaces. 

**Use Case Scenarios:**
- **Development Tools**: AI-powered development assistants
- **System Administration**: Intelligent system management interfaces  
- **Data Analysis**: AI assistants for data exploration and analysis
- **Educational**: Learning about AI integration architectures
- **Research**: Foundation for AI agent research projects

---

## 🎉 Getting Started Now

Ready to dive in? Here's the fastest path to a working system:

```bash
# 1. Prerequisites check
java -version  # Need 21+
node -v        # Need 16+
ollama --version

# 2. Quick setup
git clone <this-repository>
cd java_mcp_streaming

# 3. Start Ollama (in separate terminal)
ollama serve
ollama pull llama3.2:latest

# 4. Start everything
./start-complete-stack.sh

# 5. Open browser
open http://localhost:3000

# 6. Try it out
# Type: "List the files in the current directory"
# Watch the AI use tools to complete your request!
```

**Need help?** Check the [troubleshooting section](#troubleshooting) or run `./demo-chat.sh` to test your setup.

**Questions?** The system provides extensive logging and health checks to help debug any issues.

**Want to extend it?** Check the [development guide](#development-guide) for adding new features.

Welcome to the future of AI-powered system interaction! 🚀