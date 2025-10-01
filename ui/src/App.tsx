import React from 'react';
import { useChat } from './hooks/useChat';
import Sidebar from './components/Sidebar';
import ChatInterface from './components/ChatInterface';

function App() {
  const {
    messages,
    conversations,
    currentConversationId,
    isLoading,
    error,
    serviceHealth,
    capabilities,
    isConnected,
    sendMessage,
    startNewConversation,
    switchConversation,
    clearCurrentConversation,
    retryConnection,
  } = useChat();

  return (
    <div className="h-screen flex bg-gray-100 overflow-hidden">
      <Sidebar
        conversations={conversations}
        currentConversationId={currentConversationId}
        serviceHealth={serviceHealth}
        capabilities={capabilities}
        isConnected={isConnected}
        onNewConversation={startNewConversation}
        onSwitchConversation={switchConversation}
        onClearConversation={clearCurrentConversation}
        onRetryConnection={retryConnection}
      />
      
      <ChatInterface
        messages={messages}
        isLoading={isLoading}
        error={error}
        isConnected={isConnected}
        currentConversationId={currentConversationId}
        onSendMessage={sendMessage}
        onRetryConnection={retryConnection}
      />
    </div>
  );
}

export default App;
