# Llama Chat UI

A modern React-based chat interface for the Llama Chat Service with MCP tool integration. This UI provides an infinite feed chat experience with support for markdown rendering, code syntax highlighting, and real-time tool execution visualization.

## Features

### Chat Interface
- **Infinite Scroll Feed**: Smooth scrolling chat with auto-scroll to new messages
- **Markdown Support**: Full markdown rendering with syntax highlighting for code blocks
- **Tool Execution Visualization**: Real-time display of tool calls and their results
- **Message History**: Persistent conversation history with timestamps
- **Auto-resize Input**: Smart input field that expands with content

### Conversation Management
- **Multiple Conversations**: Create and switch between different chat sessions
- **Conversation Titles**: Auto-generated titles from first message
- **History Navigation**: Easy access to previous conversations
- **Conversation Clearing**: Clean up conversations when needed

### Service Integration
- **Real-time Health Monitoring**: Live status of backend services (Ollama, MCP)
- **Connection Management**: Automatic retry and error handling
- **Service Capabilities**: Display available models and tool configurations
- **Error Recovery**: Graceful handling of service interruptions

### UI/UX Features
- **Responsive Design**: Works on desktop and mobile devices
- **Collapsible Sidebar**: More screen space for chat when needed  
- **Loading States**: Visual feedback during message processing
- **Keyboard Shortcuts**: Enter to send, Shift+Enter for new lines
- **Visual Tool Indicators**: Clear display of which tools were used

## Prerequisites

- **Node.js 16+** (18+ recommended)
- **npm** or **yarn** package manager
- **Chat Service** running on port 8081
- **Modern browser** with WebSocket support

## Quick Start

### 1. Install Dependencies

```bash
cd chat-ui
npm install
```

### 2. Start Development Server

```bash
# Start UI only (Chat Service must be running)
./start-ui.sh

# Or start manually
npm start
```

### 3. Access the Interface

Open http://localhost:3000 in your browser.

## Usage Guide

### Starting a Chat

1. Click "New Conversation" to start
2. Type your message in the input field
3. Press Enter to send (Shift+Enter for new lines)
4. Watch as the AI responds and executes tools

### Example Conversations

#### File Operations
```
You: "List the files in the current directory"
AI: "I'll list the files for you..." [Uses list_directory tool]
```

#### Code Analysis
```  
You: "Read the README.md file and summarize it"
AI: "I'll read and analyze that file..." [Uses read_file tool]
```

#### System Commands
```
You: "Show me the system uptime and memory usage"  
AI: "I'll check the system status..." [Uses execute_command tool]
```

## Available Scripts

### `npm start`

Runs the app in the development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

### `npm test`

Launches the test runner in the interactive watch mode.

### `npm run build`

Builds the app for production to the `build` folder.\
It correctly bundles React in production mode and optimizes the build for the best performance.

### `./start-ui.sh`

Custom script that checks prerequisites and starts the development server.

## Troubleshooting

### Common Issues

#### UI Won't Load

```bash
# Check Node version (16+ required)
node -v

# Clear node modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Check for port conflicts
lsof -i :3000
```

#### Can't Connect to Chat Service

```bash
# Verify chat service is running
curl http://localhost:8081/api/chat/ping

# Check browser console for CORS errors
# Open DevTools → Console tab
```

## Architecture

```
┌─────────────────┐    HTTP/REST    ┌─────────────────┐
│   React UI      │ ──────────────► │  Chat Service   │
│  (Port 3000)    │                 │  (Port 8081)    │
│                 │ ◄────────────── │                 │
│  - Chat Feed    │      JSON       │  - API Routes   │
│  - Sidebar      │                 │  - Business     │
│  - Components   │                 │    Logic        │
└─────────────────┘                 └─────────────────┘
```

### Component Structure

- `App.tsx` - Main application component with state management
- `hooks/useChat.ts` - Custom hook for chat state and API interactions
- `components/ChatInterface.tsx` - Main chat area with infinite scroll
- `components/ChatMessage.tsx` - Individual message rendering
- `components/MessageInput.tsx` - Smart input component
- `components/Sidebar.tsx` - Conversation and service management
- `services/chatApi.ts` - API client for backend communication
- `types/chat.ts` - TypeScript definitions for all data models

## Configuration

### Environment Variables

Create a `.env` file in the `chat-ui` directory:

```env
# API endpoint (default: http://localhost:8081/api/chat)
REACT_APP_API_URL=http://localhost:8081/api/chat

# Development settings
REACT_APP_ENV=development
```

## License

This React UI is part of the Llama Chat Service demonstration project. Use and modify as needed for your requirements.
