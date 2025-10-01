import React, { useState, useRef, useEffect } from 'react';
import { Send, Loader } from 'lucide-react';

interface MessageInputProps {
  onSendMessage: (message: string) => void;
  isLoading: boolean;
  disabled?: boolean;
  placeholder?: string;
}

const MessageInput: React.FC<MessageInputProps> = ({ 
  onSendMessage, 
  isLoading, 
  disabled = false,
  placeholder = "Type your message... (Shift+Enter for new line)"
}) => {
  const [message, setMessage] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`;
    }
  }, [message]);

  // Focus textarea on mount
  useEffect(() => {
    if (textareaRef.current && !disabled) {
      textareaRef.current.focus();
    }
  }, [disabled]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (message.trim() && !isLoading && !disabled) {
      onSendMessage(message.trim());
      setMessage('');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setMessage(e.target.value);
  };

  return (
    <div className="border-t border-gray-200 bg-white py-4">
      <form onSubmit={handleSubmit} className="flex gap-2 items-end w-full px-2">
        <div className="flex-1 relative min-w-0 full-width-container" style={{ width: '100%' }}>
          <textarea
            ref={textareaRef}
            value={message}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            disabled={disabled || isLoading}
            className={`full-width-input w-full box-border px-3 py-4 border-2 border-gray-300 rounded-xl resize-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all min-h-[56px] max-h-40 text-base ${
              disabled || isLoading ? 'bg-gray-100 cursor-not-allowed' : 'bg-white hover:border-gray-400'
            }`}
            style={{ width: '100%', boxSizing: 'border-box' }}
            rows={1}
          />
          
          {/* Character count for long messages */}
          {message.length > 100 && (
            <div className={`absolute -top-6 right-0 text-xs ${
              message.length > 1000 ? 'text-red-500' : 'text-gray-400'
            }`}>
              {message.length}/2000
            </div>
          )}
        </div>

        <button
          type="submit"
          disabled={!message.trim() || isLoading || disabled}
          className={`px-4 py-4 rounded-xl font-medium transition-all flex items-center gap-1 min-h-[56px] flex-shrink-0 ${
            !message.trim() || isLoading || disabled
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
              : 'bg-blue-500 hover:bg-blue-600 text-white shadow-sm hover:shadow-md hover:scale-105'
          }`}
        >
          {isLoading ? (
            <>
              <Loader size={18} className="animate-spin" />
              <span className="hidden sm:inline">Sending...</span>
            </>
          ) : (
            <>
              <Send size={18} />
              <span className="hidden md:inline">Send</span>
            </>
          )}
        </button>
      </form>

      {/* Help text */}
      <div className="mt-2 px-2 text-xs text-gray-500 flex items-center justify-between">
        <span>Press Enter to send, Shift+Enter for new line</span>
        {disabled && (
          <span className="text-red-500 font-medium">Service unavailable</span>
        )}
      </div>
    </div>
  );
};

export default MessageInput;