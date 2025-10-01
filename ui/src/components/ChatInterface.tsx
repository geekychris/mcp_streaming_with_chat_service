import React, { useEffect, useRef, useState } from 'react';
import { AlertCircle, RefreshCw, MessageSquare } from 'lucide-react';
import ChatMessage from './ChatMessage';
import MessageInput from './MessageInput';
import { ChatMessage as ChatMessageType } from '../types/chat';

interface ChatInterfaceProps {
  messages: ChatMessageType[];
  isLoading: boolean;
  error: string | null;
  isConnected: boolean;
  currentConversationId: string | null;
  onSendMessage: (message: string) => void;
  onRetryConnection: () => void;
}

const ChatInterface: React.FC<ChatInterfaceProps> = ({
  messages,
  isLoading,
  error,
  isConnected,
  currentConversationId,
  onSendMessage,
  onRetryConnection,
}) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);
  const [showScrollButton, setShowScrollButton] = useState(false);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (autoScroll && messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ 
        behavior: 'smooth',
        block: 'end'
      });
    }
  }, [messages, autoScroll]);

  // Handle scroll events to determine if user is at bottom
  const handleScroll = () => {
    if (!messagesContainerRef.current) return;

    const container = messagesContainerRef.current;
    const threshold = 100; // px from bottom
    const isNearBottom = container.scrollTop + container.clientHeight >= 
                         container.scrollHeight - threshold;
    
    setAutoScroll(isNearBottom);
    setShowScrollButton(!isNearBottom && messages.length > 0);
  };

  // Scroll to bottom manually
  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ 
        behavior: 'smooth',
        block: 'end'
      });
      setAutoScroll(true);
      setShowScrollButton(false);
    }
  };

  // Welcome message for empty conversations
  const welcomeMessage = (
    <div className="flex-1 flex items-center justify-center p-8">
      <div className="text-center max-w-md">
        <MessageSquare size={48} className="mx-auto text-gray-300 mb-4" />
        <h2 className="text-xl font-semibold text-gray-700 mb-2">
          Welcome to Llama Chat
        </h2>
        <p className="text-gray-500 mb-6">
          I'm your AI assistant powered by Llama with access to powerful tools. 
          I can help you with file operations, system commands, code analysis, and much more!
        </p>
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-left">
          <h3 className="font-medium text-blue-900 mb-2">Try asking me to:</h3>
          <ul className="text-sm text-blue-700 space-y-1">
            <li>• List files in the current directory</li>
            <li>• Read and analyze code files</li>
            <li>• Create or edit files</li>
            <li>• Run system commands</li>
            <li>• Search for specific patterns</li>
          </ul>
        </div>
      </div>
    </div>
  );

  // Error display
  const errorDisplay = error && (
    <div className="mx-4 my-2 p-4 bg-red-50 border border-red-200 rounded-lg">
      <div className="flex items-start gap-3">
        <AlertCircle className="text-red-500 flex-shrink-0 mt-0.5" size={20} />
        <div className="flex-1">
          <div className="font-medium text-red-800 mb-1">Connection Error</div>
          <div className="text-red-700 text-sm mb-3">{error}</div>
          <button
            onClick={onRetryConnection}
            className="flex items-center gap-2 text-sm bg-red-100 hover:bg-red-200 text-red-800 px-3 py-1.5 rounded transition-colors"
          >
            <RefreshCw size={14} />
            Retry Connection
          </button>
        </div>
      </div>
    </div>
  );

  return (
    <div className="flex-1 flex flex-col h-full bg-white min-w-0">
      {/* Header */}
      <div className="border-b border-gray-200 p-4 bg-gray-50">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-semibold text-gray-900">
              {currentConversationId ? 'Conversation' : 'New Chat'}
            </h2>
            <div className="flex items-center gap-2 text-sm text-gray-500 mt-1">
              <div className={`w-2 h-2 rounded-full ${
                isConnected ? 'bg-green-500' : 'bg-red-500'
              }`} />
              {isConnected ? 'Connected' : 'Disconnected'}
              {messages.length > 0 && (
                <>
                  <span>•</span>
                  <span>{messages.length} messages</span>
                </>
              )}
            </div>
          </div>
          
          {/* Loading indicator */}
          {isLoading && (
            <div className="flex items-center gap-2 text-sm text-gray-600">
              <div className="animate-spin w-4 h-4 border-2 border-gray-300 border-t-blue-500 rounded-full" />
              Thinking...
            </div>
          )}
        </div>
      </div>

      {/* Error display */}
      {errorDisplay}

      {/* Messages area */}
      <div 
        ref={messagesContainerRef}
        onScroll={handleScroll}
        className="flex-1 overflow-y-auto"
        style={{ scrollBehavior: 'smooth' }}
      >
        {messages.length === 0 ? (
          welcomeMessage
        ) : (
          <div className="divide-y divide-gray-100">
            {messages.map((message, index) => (
              <ChatMessage 
                key={message.id} 
                message={message} 
                isLast={index === messages.length - 1}
              />
            ))}
          </div>
        )}
        
        {/* Scroll anchor */}
        <div ref={messagesEndRef} />
      </div>

      {/* Scroll to bottom button */}
      {showScrollButton && (
        <div className="absolute bottom-20 right-6">
          <button
            onClick={scrollToBottom}
            className="bg-blue-500 hover:bg-blue-600 text-white p-3 rounded-full shadow-lg transition-colors"
            title="Scroll to bottom"
          >
            <svg 
              className="w-4 h-4" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
            >
              <path 
                strokeLinecap="round" 
                strokeLinejoin="round" 
                strokeWidth={2} 
                d="M19 14l-7 7m0 0l-7-7m7 7V3" 
              />
            </svg>
          </button>
        </div>
      )}

      {/* Message input */}
      <MessageInput
        onSendMessage={onSendMessage}
        isLoading={isLoading}
        disabled={!isConnected}
        placeholder={
          !isConnected 
            ? "Service unavailable - check connection" 
            : isLoading 
              ? "Please wait..." 
              : "Type your message... (Shift+Enter for new line)"
        }
      />
    </div>
  );
};

export default ChatInterface;