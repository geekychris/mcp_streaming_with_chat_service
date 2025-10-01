import React, { useState } from 'react';
import { 
  Plus, 
  MessageSquare, 
  Trash2, 
  Settings, 
  Activity, 
  Wrench,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  CheckCircle
} from 'lucide-react';
import { Conversation, ServiceHealth, ServiceCapabilities } from '../types/chat';
import chatApiService from '../services/chatApi';

interface SidebarProps {
  conversations: Conversation[];
  currentConversationId: string | null;
  serviceHealth: ServiceHealth | null;
  capabilities: ServiceCapabilities | null;
  isConnected: boolean;
  onNewConversation: () => void;
  onSwitchConversation: (id: string) => void;
  onClearConversation: () => void;
  onRetryConnection: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({
  conversations,
  currentConversationId,
  serviceHealth,
  capabilities,
  isConnected,
  onNewConversation,
  onSwitchConversation,
  onClearConversation,
  onRetryConnection,
}) => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [showSettings, setShowSettings] = useState(false);

  const getHealthStatusColor = () => {
    if (!serviceHealth) return 'text-gray-400';
    switch (serviceHealth.status) {
      case 'healthy': return 'text-green-500';
      case 'degraded': return 'text-yellow-500';
      case 'error': return 'text-red-500';
      default: return 'text-gray-400';
    }
  };

  const getHealthIcon = () => {
    if (!serviceHealth) return <AlertCircle size={16} />;
    switch (serviceHealth.status) {
      case 'healthy': return <CheckCircle size={16} />;
      case 'degraded': return <AlertCircle size={16} />;
      case 'error': return <AlertCircle size={16} />;
      default: return <AlertCircle size={16} />;
    }
  };

  if (isCollapsed) {
    return (
      <div className="w-16 bg-gray-50 border-r border-gray-200 flex flex-col">
        <div className="p-4">
          <button
            onClick={() => setIsCollapsed(false)}
            className="w-8 h-8 bg-blue-500 text-white rounded-lg flex items-center justify-center hover:bg-blue-600 transition-colors"
          >
            <ChevronRight size={16} />
          </button>
        </div>
        
        <div className="flex-1 flex flex-col items-center gap-2 p-2">
          <button
            onClick={onNewConversation}
            className="w-10 h-10 bg-gray-200 hover:bg-gray-300 rounded-lg flex items-center justify-center transition-colors"
            title="New Conversation"
          >
            <Plus size={16} />
          </button>
          
          {conversations.slice(0, 8).map((conv) => (
            <button
              key={conv.id}
              onClick={() => onSwitchConversation(conv.id)}
              className={`w-10 h-10 rounded-lg flex items-center justify-center transition-colors ${
                currentConversationId === conv.id
                  ? 'bg-blue-100 text-blue-600'
                  : 'bg-gray-200 hover:bg-gray-300 text-gray-600'
              }`}
              title={conv.title}
            >
              <MessageSquare size={16} />
            </button>
          ))}
        </div>

        <div className="p-2">
          <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${getHealthStatusColor()}`}>
            {getHealthIcon()}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="w-64 md:w-64 bg-gray-50 border-r border-gray-200 flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between mb-3">
          <h1 className="text-lg font-semibold text-gray-900">Llama Chat</h1>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="p-1 hover:bg-gray-200 rounded transition-colors"
              title="Settings"
            >
              <Settings size={16} />
            </button>
            <button
              onClick={() => setIsCollapsed(true)}
              className="p-1 hover:bg-gray-200 rounded transition-colors"
              title="Collapse sidebar"
            >
              <ChevronLeft size={16} />
            </button>
          </div>
        </div>

        <button
          onClick={onNewConversation}
          className="w-full bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
        >
          <Plus size={18} />
          New Conversation
        </button>
      </div>

      {/* Service Status */}
      <div className="px-4 py-3 border-b border-gray-200">
        <div className="flex items-center gap-2 mb-2">
          <div className={getHealthStatusColor()}>
            {getHealthIcon()}
          </div>
          <span className="text-sm font-medium">
            Service Status: {serviceHealth?.status || 'Unknown'}
          </span>
        </div>
        
        {!isConnected && (
          <button
            onClick={onRetryConnection}
            className="text-xs text-blue-600 hover:text-blue-800 underline"
          >
            Retry Connection
          </button>
        )}
        
        {capabilities && (
          <div className="text-xs text-gray-600 mt-1">
            {capabilities.models.length} models • 
            {capabilities.toolsEnabled ? ' Tools enabled' : ' Tools disabled'}
          </div>
        )}
      </div>

      {/* Settings Panel */}
      {showSettings && (
        <div className="px-4 py-3 border-b border-gray-200 bg-gray-100">
          <h3 className="text-sm font-medium mb-2">Service Info</h3>
          <div className="space-y-1 text-xs text-gray-600">
            {serviceHealth?.services && (
              <>
                <div className="flex items-center gap-2">
                  <Activity size={12} />
                  <span>
                    Ollama: {serviceHealth.services.ollama?.healthy ? '✓' : '✗'}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <Wrench size={12} />
                  <span>
                    MCP: {serviceHealth.services.mcp?.healthy ? '✓' : '✗'}
                  </span>
                </div>
              </>
            )}
            {capabilities && (
              <>
                <div>Models: {capabilities.models.join(', ')}</div>
                <div>Max tool calls: {capabilities.maxToolCallsPerTurn}</div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Conversations List */}
      <div className="flex-1 overflow-y-auto">
        <div className="p-4">
          <h2 className="text-sm font-medium text-gray-700 mb-3">
            Conversations ({conversations.length})
          </h2>
          
          {conversations.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <MessageSquare size={24} className="mx-auto mb-2 opacity-50" />
              <p className="text-sm">No conversations yet</p>
              <p className="text-xs mt-1">Start a new chat to get going!</p>
            </div>
          ) : (
            <div className="space-y-2">
              {conversations.map((conversation) => (
                <div
                  key={conversation.id}
                  className={`relative group p-3 rounded-lg cursor-pointer transition-colors ${
                    currentConversationId === conversation.id
                      ? 'bg-blue-100 border border-blue-200'
                      : 'bg-white hover:bg-gray-100 border border-gray-200'
                  }`}
                  onClick={() => onSwitchConversation(conversation.id)}
                >
                  <div className="flex items-start gap-2">
                    <MessageSquare 
                      size={16} 
                      className={`flex-shrink-0 mt-0.5 ${
                        currentConversationId === conversation.id
                          ? 'text-blue-600'
                          : 'text-gray-400'
                      }`} 
                    />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium text-gray-900 truncate">
                        {conversation.title}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        {conversation.messages.length} messages • 
                        {chatApiService.formatTimestamp(conversation.lastActivity)}
                      </div>
                    </div>
                  </div>

                  {/* Delete button */}
                  {currentConversationId === conversation.id && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onClearConversation();
                      }}
                      className="absolute top-2 right-2 p-1 bg-red-100 hover:bg-red-200 text-red-600 rounded opacity-0 group-hover:opacity-100 transition-opacity"
                      title="Clear conversation"
                    >
                      <Trash2 size={12} />
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Footer */}
      <div className="p-4 border-t border-gray-200">
        <div className="text-xs text-gray-500 text-center">
          Powered by Llama & MCP Tools
        </div>
      </div>
    </div>
  );
};

export default Sidebar;