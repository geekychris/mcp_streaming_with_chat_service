export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  conversationId?: string;
  toolCallResults?: ToolCallResult[];
}

export interface ToolCallResult {
  id: string;
  toolName: string;
  success: boolean;
  result?: any;
  error?: string;
}

export interface ChatRequest {
  message: string;
  conversationId?: string;
  model?: string;
  enableTools?: boolean;
  temperature?: number;
  maxTokens?: number;
}

export interface ChatResponse {
  message: ChatMessage;
  conversationId: string;
  modelUsed: string;
  toolCallsMade?: ToolCallResult[];
  processingTimeMs: number;
  timestamp: string;
}

export interface ServiceHealth {
  status: 'healthy' | 'degraded' | 'error';
  services?: {
    ollama?: { healthy: boolean };
    mcp?: { healthy: boolean };
  };
  toolsEnabled?: boolean;
}

export interface ServiceCapabilities {
  models: string[];
  toolsEnabled: boolean;
  mcpOperations: any;
  maxToolCallsPerTurn: number;
}

export interface Conversation {
  id: string;
  messages: ChatMessage[];
  lastActivity: string;
  title?: string;
}