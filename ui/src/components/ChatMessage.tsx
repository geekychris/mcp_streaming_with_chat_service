import React from 'react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { User, Bot, Wrench, Clock, CheckCircle, XCircle } from 'lucide-react';
import { ChatMessage as ChatMessageType } from '../types/chat';
import chatApiService from '../services/chatApi';

interface ChatMessageProps {
  message: ChatMessageType;
  isLast?: boolean;
}

const ChatMessage: React.FC<ChatMessageProps> = ({ message, isLast }) => {
  const isUser = message.role === 'user';
  const toolCallSummary = chatApiService.getToolCallSummary(message.toolCallResults);

  return (
    <div className={`flex gap-4 p-4 ${isUser ? 'bg-blue-50' : 'bg-white'} ${isLast ? 'mb-4' : ''}`}>
      {/* Avatar */}
      <div className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
        isUser ? 'bg-blue-500 text-white' : 'bg-gray-600 text-white'
      }`}>
        {isUser ? <User size={16} /> : <Bot size={16} />}
      </div>

      {/* Message content */}
      <div className="flex-1 min-w-0">
        {/* Header with role and timestamp */}
        <div className="flex items-center gap-2 mb-2">
          <span className="font-medium text-sm text-gray-900">
            {isUser ? 'You' : 'Assistant'}
          </span>
          <div className="flex items-center gap-1 text-xs text-gray-500">
            <Clock size={12} />
            {chatApiService.formatTimestamp(message.timestamp)}
          </div>
          {toolCallSummary && (
            <div className="flex items-center gap-1 text-xs text-green-600 bg-green-100 px-2 py-1 rounded-full">
              <Wrench size={12} />
              {toolCallSummary}
            </div>
          )}
        </div>

        {/* Message text */}
        <div className="prose prose-sm max-w-none">
          {chatApiService.hasCodeContent(message.content) ? (
            <ReactMarkdown
              components={{
                code: ({ className, children, ...props }: any) => {
                  const match = /language-(\w+)/.exec(className || '');
                  const language = match ? match[1] : '';
                  const isInline = !className;
                  
                  return !isInline && language ? (
                    <SyntaxHighlighter
                      style={vscDarkPlus as any}
                      language={language}
                      PreTag="div"
                      className="rounded-md"
                      {...props}
                    >
                      {String(children).replace(/\n$/, '')}
                    </SyntaxHighlighter>
                  ) : (
                    <code className="bg-gray-100 px-1 py-0.5 rounded text-sm" {...props}>
                      {children}
                    </code>
                  );
                },
                pre: ({ children }: any) => <div>{children}</div>,
              }}
            >
              {message.content}
            </ReactMarkdown>
          ) : (
            <p className="text-gray-900 whitespace-pre-wrap">{message.content}</p>
          )}
        </div>

        {/* Tool call results */}
        {message.toolCallResults && message.toolCallResults.length > 0 && (
          <div className="mt-3 space-y-2">
            <div className="text-xs font-medium text-gray-700 mb-2">Tool Execution Details:</div>
            {message.toolCallResults.map((result) => (
              <div 
                key={result.id} 
                className={`flex items-start gap-2 p-2 rounded-md text-xs ${
                  result.success 
                    ? 'bg-green-50 border border-green-200' 
                    : 'bg-red-50 border border-red-200'
                }`}
              >
                <div className={`flex-shrink-0 mt-0.5 ${
                  result.success ? 'text-green-600' : 'text-red-600'
                }`}>
                  {result.success ? <CheckCircle size={14} /> : <XCircle size={14} />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className={`font-medium ${
                    result.success ? 'text-green-800' : 'text-red-800'
                  }`}>
                    {result.toolName}
                  </div>
                  {result.success && result.result && (
                    <div className="text-green-700 mt-1">
                      {typeof result.result === 'string' 
                        ? result.result 
                        : JSON.stringify(result.result, null, 2)}
                    </div>
                  )}
                  {!result.success && result.error && (
                    <div className="text-red-700 mt-1">{result.error}</div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ChatMessage;