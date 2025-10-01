import { ChatMessage, ChatRequest, ChatResponse, ServiceHealth, ServiceCapabilities } from '../types/chat';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8081/api/chat';

class ChatApiService {
  private async fetchWithErrorHandling<T>(url: string, options?: RequestInit): Promise<T> {
    try {
      const response = await fetch(url, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...options?.headers,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  async sendMessage(request: ChatRequest): Promise<ChatResponse> {
    return this.fetchWithErrorHandling<ChatResponse>(`${API_BASE_URL}/message`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getConversationHistory(conversationId: string): Promise<ChatMessage[]> {
    return this.fetchWithErrorHandling<ChatMessage[]>(
      `${API_BASE_URL}/conversation/${conversationId}/history`
    );
  }

  async clearConversation(conversationId: string): Promise<{ message: string; conversation_id: string }> {
    return this.fetchWithErrorHandling(`${API_BASE_URL}/conversation/${conversationId}`, {
      method: 'DELETE',
    });
  }

  async getActiveConversations(): Promise<{ conversations: string[]; count: number }> {
    return this.fetchWithErrorHandling(`${API_BASE_URL}/conversations`);
  }

  async getServiceHealth(): Promise<ServiceHealth> {
    return this.fetchWithErrorHandling<ServiceHealth>(`${API_BASE_URL}/health`);
  }

  async getServiceCapabilities(): Promise<ServiceCapabilities> {
    return this.fetchWithErrorHandling<ServiceCapabilities>(`${API_BASE_URL}/capabilities`);
  }

  async ping(): Promise<{ message: string; timestamp: number; service: string }> {
    return this.fetchWithErrorHandling(`${API_BASE_URL}/ping`);
  }

  // Utility method to generate conversation title from first message
  generateConversationTitle(message: string): string {
    const words = message.trim().split(' ');
    const title = words.slice(0, 5).join(' ');
    return title.length > 30 ? title.substring(0, 27) + '...' : title;
  }

  // Utility method to format timestamps
  formatTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) return 'Just now';
    if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
    
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours}h ago`;
    
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) return `${diffInDays}d ago`;
    
    return date.toLocaleDateString();
  }

  // Utility method to check if a message contains code
  hasCodeContent(content: string): boolean {
    return content.includes('```') || content.includes('`');
  }

  // Utility method to extract tool call information
  getToolCallSummary(toolCallResults?: any[]): string {
    if (!toolCallResults || toolCallResults.length === 0) {
      return '';
    }

    const toolNames = toolCallResults.map(result => result.toolName);
    const uniqueTools = Array.from(new Set(toolNames));
    
    if (uniqueTools.length === 1) {
      return `Used ${uniqueTools[0]}`;
    }
    
    return `Used ${uniqueTools.length} tools: ${uniqueTools.join(', ')}`;
  }
}

export const chatApiService = new ChatApiService();
export default chatApiService;