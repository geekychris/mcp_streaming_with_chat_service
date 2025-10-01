import { useState, useCallback, useRef, useEffect } from 'react';
import { ChatMessage, ChatRequest, ServiceHealth, ServiceCapabilities, Conversation } from '../types/chat';
import chatApiService from '../services/chatApi';

interface UseChatState {
  messages: ChatMessage[];
  conversations: Conversation[];
  currentConversationId: string | null;
  isLoading: boolean;
  error: string | null;
  serviceHealth: ServiceHealth | null;
  capabilities: ServiceCapabilities | null;
  isConnected: boolean;
}

interface UseChatActions {
  sendMessage: (message: string, options?: Partial<ChatRequest>) => Promise<void>;
  startNewConversation: () => void;
  switchConversation: (conversationId: string) => Promise<void>;
  clearCurrentConversation: () => Promise<void>;
  refreshHealth: () => Promise<void>;
  loadCapabilities: () => Promise<void>;
  retryConnection: () => Promise<void>;
}

interface UseChatReturn extends UseChatState, UseChatActions {}

export const useChat = (): UseChatReturn => {
  const [state, setState] = useState<UseChatState>({
    messages: [],
    conversations: [],
    currentConversationId: null,
    isLoading: false,
    error: null,
    serviceHealth: null,
    capabilities: null,
    isConnected: false,
  });

  const conversationsRef = useRef<Map<string, ChatMessage[]>>(new Map());
  const abortControllerRef = useRef<AbortController | null>(null);

  // Initialize chat on component mount
  useEffect(() => {
    const initializeChat = async () => {
      await Promise.all([
        refreshHealth(),
        loadCapabilities(),
      ]);
    };
    
    initializeChat();
    
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Remove initializeChat from dependencies to prevent loop

  const updateState = useCallback((updates: Partial<UseChatState>) => {
    setState(prev => ({ ...prev, ...updates }));
  }, []);

  const updateConversationsList = useCallback((conversationId: string, messages: ChatMessage[]) => {
    setState(prev => {
      const existingIndex = prev.conversations.findIndex(c => c.id === conversationId);
      const lastMessage = messages[messages.length - 1];
      const title = messages.length > 0 ? 
        chatApiService.generateConversationTitle(messages[0].content) : 
        'New Conversation';
      
      const conversation: Conversation = {
        id: conversationId,
        messages,
        lastActivity: lastMessage?.timestamp || new Date().toISOString(),
        title,
      };
      
      const newConversations = [...prev.conversations];
      if (existingIndex >= 0) {
        newConversations[existingIndex] = conversation;
      } else {
        newConversations.unshift(conversation);
      }
      
      // Sort by last activity
      newConversations.sort((a, b) => 
        new Date(b.lastActivity).getTime() - new Date(a.lastActivity).getTime()
      );
      
      return { ...prev, conversations: newConversations };
    });
  }, []);

  const addMessage = useCallback((message: ChatMessage) => {
    const conversationId = message.conversationId || state.currentConversationId || 'default';
    
    // Update messages in memory
    const conversationMessages = conversationsRef.current.get(conversationId) || [];
    conversationMessages.push(message);
    conversationsRef.current.set(conversationId, conversationMessages);
    
    // Always update the state with the messages - this ensures UI updates immediately
    updateState({ messages: [...conversationMessages] });
    
    // Update conversations list
    updateConversationsList(conversationId, conversationMessages);
  }, [state.currentConversationId, updateState, updateConversationsList]);

  const sendMessage = useCallback(async (message: string, options: Partial<ChatRequest> = {}) => {
    if (!message.trim()) return;

    // Start new conversation if none exists
    let conversationId = state.currentConversationId;
    if (!conversationId) {
      conversationId = `conv-${Date.now()}`;
      updateState({ currentConversationId: conversationId });
    }

    // Cancel any ongoing request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();

    updateState({ isLoading: true, error: null });

    // Create user message
    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: message.trim(),
      timestamp: new Date().toISOString(),
      conversationId: conversationId,
    };


    // Add user message to chat
    addMessage(userMessage);

    try {
      const request: ChatRequest = {
        message: message.trim(),
        conversationId: conversationId,
        enableTools: true,
        ...options,
      };

      const response = await chatApiService.sendMessage(request);
      
      // Update current conversation ID if it was generated
      if (!state.currentConversationId && response.conversationId) {
        updateState({ currentConversationId: response.conversationId });
      }

      // Add assistant response
      addMessage(response.message);

    } catch (error) {
      console.error('Failed to send message:', error);
      updateState({ 
        error: error instanceof Error ? error.message : 'Failed to send message' 
      });

      // Add error message to chat
      const errorMessage: ChatMessage = {
        id: `error-${Date.now()}`,
        role: 'assistant',
        content: `Sorry, I encountered an error: ${error instanceof Error ? error.message : 'Unknown error'}`,
        timestamp: new Date().toISOString(),
        conversationId: state.currentConversationId || undefined,
      };
      addMessage(errorMessage);
    } finally {
      updateState({ isLoading: false });
      abortControllerRef.current = null;
    }
  }, [state.currentConversationId, addMessage, updateState]);

  const startNewConversation = useCallback(() => {
    const newConversationId = `conv-${Date.now()}`;
    updateState({ 
      currentConversationId: newConversationId,
      messages: [],
      error: null 
    });
  }, [updateState]);

  const switchConversation = useCallback(async (conversationId: string) => {
    updateState({ isLoading: true, error: null });

    try {
      // Load conversation from memory first
      let messages = conversationsRef.current.get(conversationId) || [];
      
      // If not in memory, try to load from backend
      if (messages.length === 0) {
        try {
          messages = await chatApiService.getConversationHistory(conversationId);
          conversationsRef.current.set(conversationId, messages);
        } catch (error) {
          console.warn('Could not load conversation history:', error);
        }
      }

      updateState({ 
        currentConversationId: conversationId,
        messages,
      });
    } catch (error) {
      console.error('Failed to switch conversation:', error);
      updateState({ 
        error: error instanceof Error ? error.message : 'Failed to load conversation' 
      });
    } finally {
      updateState({ isLoading: false });
    }
  }, [updateState]);

  const clearCurrentConversation = useCallback(async () => {
    if (!state.currentConversationId) return;

    updateState({ isLoading: true, error: null });

    try {
      await chatApiService.clearConversation(state.currentConversationId);
      
      // Remove from memory
      conversationsRef.current.delete(state.currentConversationId);
      
      // Update state
      setState(prev => ({
        ...prev,
        messages: [],
        conversations: prev.conversations.filter(c => c.id !== state.currentConversationId),
        currentConversationId: null,
        isLoading: false,
      }));
    } catch (error) {
      console.error('Failed to clear conversation:', error);
      updateState({ 
        error: error instanceof Error ? error.message : 'Failed to clear conversation',
        isLoading: false,
      });
    }
  }, [state.currentConversationId, updateState]);

  const refreshHealth = useCallback(async () => {
    try {
      const health = await chatApiService.getServiceHealth();
      updateState({ 
        serviceHealth: health,
        isConnected: health.status === 'healthy',
        error: health.status === 'healthy' ? null : `Service status: ${health.status}`,
      });
    } catch (error) {
      console.error('Health check failed:', error);
      updateState({ 
        serviceHealth: { status: 'error' },
        isConnected: false,
        error: 'Cannot connect to chat service',
      });
    }
  }, [updateState]);

  const loadCapabilities = useCallback(async () => {
    try {
      const capabilities = await chatApiService.getServiceCapabilities();
      updateState({ capabilities });
    } catch (error) {
      console.warn('Failed to load capabilities (non-critical):', error);
      // Set a fallback capabilities object so the app doesn't break
      updateState({ 
        capabilities: {
          models: ['llama3.2:latest'], // Default model
          toolsEnabled: true,
          mcpOperations: 'unavailable',
          maxToolCallsPerTurn: 5
        }
      });
    }
  }, [updateState]);

  const retryConnection = useCallback(async () => {
    updateState({ error: null });
    await refreshHealth();
  }, [refreshHealth, updateState]);

  return {
    ...state,
    sendMessage,
    startNewConversation,
    switchConversation,
    clearCurrentConversation,
    refreshHealth,
    loadCapabilities,
    retryConnection,
  };
};