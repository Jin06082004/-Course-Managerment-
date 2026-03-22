/**
 * Toast Notification System
 * Shows non-blocking notifications (success, error, warning, info)
 */

window.ToastManager = {
    container: null,

    init() {
        if (this.container) return;
        const existing = document.getElementById('toastContainer');
        if (existing) {
            this.container = existing;
            return;
        }
        this.container = document.createElement('div');
        this.container.id = 'toastContainer';
        this.container.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            z-index: 9999;
            display: flex;
            flex-direction: column;
            gap: 10px;
            pointer-events: none;
            max-width: 380px;
            width: calc(100vw - 40px);
        `;
        document.body.appendChild(this.container);
    },

    show(message, type = 'info', duration = 3500) {
        this.init();
        const toast = document.createElement('div');
        const icons = {
            success: 'check-circle',
            error: 'alert-circle',
            warning: 'alert-triangle',
            info: 'info'
        };
        const colors = {
            success: { bg: 'rgba(16, 185, 129, 0.12)', border: 'rgba(16, 185, 129, 0.5)', icon: '#10b981', text: '#6ee7b7' },
            error: { bg: 'rgba(239, 68, 68, 0.12)', border: 'rgba(239, 68, 68, 0.5)', icon: '#ef4444', text: '#fca5a5' },
            warning: { bg: 'rgba(251, 191, 36, 0.12)', border: 'rgba(251, 191, 36, 0.5)', icon: '#f59e0b', text: '#fde68a' },
            info: { bg: 'rgba(124, 58, 237, 0.12)', border: 'rgba(124, 58, 237, 0.5)', icon: '#7c3aed', text: '#c4b5fd' }
        };
        const c = colors[type] || colors.info;

        toast.style.cssText = `
            background: ${c.bg};
            border: 1px solid ${c.border};
            border-radius: 12px;
            padding: 14px 16px;
            display: flex;
            align-items: flex-start;
            gap: 12px;
            pointer-events: auto;
            animation: toastSlideIn 0.3s ease forwards;
            backdrop-filter: blur(10px);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
        `;
        toast.innerHTML = `
            <span style="color: ${c.icon}; flex-shrink: 0; margin-top: 2px;">
                <i data-feather="${icons[type] || 'info'}" style="width: 20px; height: 20px;"></i>
            </span>
            <span style="color: ${c.text}; font-size: 0.9rem; line-height: 1.5; flex: 1;">${message}</span>
            <button onclick="this.parentElement.remove()" style="
                background: none; border: none; color: ${c.text};
                cursor: pointer; padding: 2px; opacity: 0.7;
                flex-shrink: 0; line-height: 1;
            ">
                <i data-feather="x" style="width: 16px; height: 16px;"></i>
            </button>
        `;
        this.container.appendChild(toast);
        if (typeof feather !== 'undefined') feather.replace();

        // Auto remove
        setTimeout(() => {
            toast.style.animation = 'toastSlideOut 0.3s ease forwards';
            setTimeout(() => toast.remove(), 300);
        }, duration);
    },

    success(message, duration) { this.show(message, 'success', duration); },
    error(message, duration) { this.show(message, 'error', duration); },
    warning(message, duration) { this.show(message, 'warning', duration); },
    info(message, duration) { this.show(message, 'info', duration); }
};

// Add toast animation styles once
const toastStyle = document.createElement('style');
toastStyle.textContent = `
@keyframes toastSlideIn {
    from { opacity: 0; transform: translateX(100%); }
    to { opacity: 1; transform: translateX(0); }
}
@keyframes toastSlideOut {
    from { opacity: 1; transform: translateX(0); }
    to { opacity: 0; transform: translateX(100%); }
}
`;
document.head.appendChild(toastStyle);
