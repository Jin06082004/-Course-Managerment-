// AI Chat Functionality

let aiChatState = {
    isOpen: false,
    lessonId: null,
    messages: [],
    isLoading: false,
    token: null
};

// Initialize AI Chat
function initAIChat() {
    aiChatState.token = localStorage.getItem('token');
    aiChatState.lessonId = getLessonIdFromUrl();
    
    if (aiChatState.lessonId) {
        loadChatHistory();
        loadSuggestedQuestions();
    }

    // Auto-resize textarea
    const textarea = document.getElementById('aiMessageInput');
    if (textarea) {
        textarea.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 100) + 'px';
        });
    }
}

/** Get lesson ID from URL */
function getLessonIdFromUrl() {
    // Extract from the current page - lessonId is passed from backend
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('lessonId') || window.activeLessonId;
}

/** Toggle AI Chat visibility */
function toggleAIChat() {
    const sidebar = document.getElementById('aiChatSidebar');
    const toggleBtn = document.getElementById('aiChatToggleBtn');
    
    aiChatState.isOpen = !aiChatState.isOpen;
    
    if (aiChatState.isOpen) {
        sidebar.classList.remove('hidden');
        toggleBtn.classList.add('hidden');
        // Scroll to bottom
        setTimeout(() => {
            const chatMessages = document.getElementById('chatMessages');
            chatMessages.scrollTop = chatMessages.scrollHeight;
        }, 100);
    } else {
        sidebar.classList.add('hidden');
        toggleBtn.classList.remove('hidden');
    }
}

/** Load chat history from API */
function loadChatHistory() {
    if (!aiChatState.lessonId) return;

    const url = `/api/ai/history/${aiChatState.lessonId}`;
    
    fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${aiChatState.token}`,
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success && data.messages && data.messages.length > 0) {
            displayChatMessages(data.messages);
        }
    })
    .catch(error => console.error('Error loading chat history:', error));
}

/** Display chat messages in UI */
function displayChatMessages(messages) {
    const chatMessages = document.getElementById('chatMessages');
    
    // Clear welcome message if there are messages
    if (messages.length > 0) {
        chatMessages.innerHTML = '';
    }

    messages.forEach(msg => {
        // Display user message
        if (msg.senderType === 'user') {
            appendChatMessage(msg.content, 'user', msg.createdAt);
        }
        // Display AI response
        if (msg.aiResponse) {
            appendChatMessage(msg.aiResponse, 'assistant', msg.createdAt);
        }
    });

    // Scroll to bottom
    setTimeout(() => {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }, 100);
}

/** Append message to chat */
function appendChatMessage(content, sender, timestamp = null) {
    const chatMessages = document.getElementById('chatMessages');
    
    // Remove welcome message if it's the first message
    const welcome = chatMessages.querySelector('.ai-chat-welcome');
    if (welcome) {
        welcome.remove();
    }

    const messageDiv = document.createElement('div');
    messageDiv.className = `ai-chat-message ${sender}`;
    
    let timeStr = '';
    if (timestamp) {
        timeStr = new Date(timestamp).toLocaleTimeString('en-US', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }

    messageDiv.innerHTML = `
        <div class="ai-chat-bubble">
            ${escapeHtml(content)}
        </div>
        ${timeStr ? `<div class="ai-chat-timestamp">${escapeHtml(timeStr)}</div>` : ''}
    `;

    chatMessages.appendChild(messageDiv);
    
    // Scroll to bottom
    setTimeout(() => {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }, 50);
}

/** Append loading indicator */
function appendLoadingMessage() {
    const chatMessages = document.getElementById('chatMessages');
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'ai-chat-message assistant';
    messageDiv.id = 'loading-message';
    
    messageDiv.innerHTML = `
        <div class="ai-chat-bubble loading">
            <span class="ai-chat-loading-dots">
                <span class="ai-chat-loading-dot"></span>
                <span class="ai-chat-loading-dot"></span>
                <span class="ai-chat-loading-dot"></span>
            </span>
        </div>
    `;

    chatMessages.appendChild(messageDiv);
    
    // Scroll to bottom
    setTimeout(() => {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }, 50);
}

/** Send message to AI */
function sendAIMessage() {
    const input = document.getElementById('aiMessageInput');
    const message = input.value.trim();

    if (!message || aiChatState.isLoading || !aiChatState.lessonId) {
        return;
    }

    // Add user message to chat
    appendChatMessage(message, 'user');

    // Clear input
    input.value = '';
    input.style.height = 'auto';

    // Show loading
    appendLoadingMessage();
    aiChatState.isLoading = true;

    // Send to API
    const payload = {
        lessonId: aiChatState.lessonId,
        message: message
    };

    fetch('/api/ai/chat', {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${aiChatState.token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
    .then(response => response.json())
    .then(data => {
        // Remove loading indicator
        const loadingMsg = document.getElementById('loading-message');
        if (loadingMsg) loadingMsg.remove();

        if (data.success && data.aiResponse) {
            appendChatMessage(data.aiResponse, 'assistant');
        } else {
            const error = data.error || 'Failed to get response';
            appendErrorMessage(error);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        const loadingMsg = document.getElementById('loading-message');
        if (loadingMsg) loadingMsg.remove();
        appendErrorMessage('Error: Unable to connect to AI service');
    })
    .finally(() => {
        aiChatState.isLoading = false;
        document.getElementById('aiMessageInput').focus();
    });
}

/** Append error message */
function appendErrorMessage(error) {
    const chatMessages = document.getElementById('chatMessages');
    
    const messageDiv = document.createElement('div');
    messageDiv.className = 'ai-chat-error-message';
    messageDiv.textContent = error;

    chatMessages.appendChild(messageDiv);
    
    // Remove error after 5 seconds
    setTimeout(() => {
        messageDiv.remove();
    }, 5000);
}

/** Handle keyboard input */
function handleChatKeypress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendAIMessage();
    }
}

/** Load suggested questions */
function loadSuggestedQuestions() {
    if (!aiChatState.lessonId) return;

    const url = `/api/ai/suggestions/${aiChatState.lessonId}`;
    
    fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(suggestions => {
        displaySuggestedQuestions(suggestions);
    })
    .catch(error => console.error('Error loading suggestions:', error));
}

/** Display suggested questions */
function displaySuggestedQuestions(suggestions) {
    const container = document.getElementById('suggestedQuestions');
    const section = document.getElementById('suggestedSection');
    
    if (!container || !suggestions || suggestions.length === 0) return;

    container.innerHTML = '';
    
    suggestions.slice(0, 4).forEach(question => {
        const btn = document.createElement('button');
        btn.className = 'ai-chat-suggestion-btn';
        btn.textContent = question;
        btn.onclick = () => {
            document.getElementById('aiMessageInput').value = question;
            document.getElementById('aiMessageInput').focus();
        };
        container.appendChild(btn);
    });

    section.classList.remove('hidden');
}

/** Clear chat history */
function clearAIChatHistory() {
    if (!aiChatState.lessonId) return;

    const confirmClear = confirm('Are you sure you want to clear all chat history for this lesson?');
    if (!confirmClear) return;

    const url = `/api/ai/history/${aiChatState.lessonId}`;
    
    fetch(url, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${aiChatState.token}`,
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // Clear messages
            const chatMessages = document.getElementById('chatMessages');
            chatMessages.innerHTML = `
                <div class="ai-chat-welcome">
                    <p>👋 Ask me anything about this lesson!</p>
                    <p class="ai-chat-hint">I can help explain concepts, answer your questions, and provide examples.</p>
                </div>
            `;
            showToast('Chat history cleared');
        }
    })
    .catch(error => {
        console.error('Error clearing history:', error);
        showToast('Error clearing chat history', 'error');
    });
}

/** Escape HTML to prevent XSS */
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

/** Initialize when DOM is ready */
document.addEventListener('DOMContentLoaded', function() {
    initAIChat();
});

// Export for external use if needed
window.AIChat = {
    init: initAIChat,
    toggle: toggleAIChat,
    sendMessage: sendAIMessage,
    clearHistory: clearAIChatHistory
};
