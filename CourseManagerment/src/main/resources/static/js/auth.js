/**
 * Authentication Management
 * Handles login/registration and navbar UI updates
 */

const AuthManager = {
    TOKEN_KEY: 'authToken',
    USER_KEY: 'userInfo',

    /**
     * Save token and user info to localStorage
     */
    saveAuth(token, userInfo) {
        localStorage.setItem(this.TOKEN_KEY, token);
        localStorage.setItem(this.USER_KEY, JSON.stringify(userInfo));
        this.updateNavbar(userInfo);
    },

    /**
     * Get stored token
     */
    getToken() {
        return localStorage.getItem(this.TOKEN_KEY);
    },

    /**
     * Get stored user info
     */
    getUser() {
        const user = localStorage.getItem(this.USER_KEY);
        return user ? JSON.parse(user) : null;
    },

    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        return !!this.getToken();
    },

    /**
     * Clear authentication data from localStorage
     */
    clearAuth() {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
        this.updateNavbar(null);
    },

    /**
     * Logout - call server endpoint then clear local storage
     */
    logout() {
        const token = this.getToken();
        
        // Call logout endpoint on server
        fetch('/api/auth/logout', {
            method: 'POST',
            headers: this.getAuthHeaders()
        })
        .then(response => {
            if (!response.ok) {
                console.warn('Server logout failed, but clearing local storage anyway');
            }
        })
        .catch(error => {
            console.warn('Logout endpoint call failed:', error);
        })
        .finally(() => {
            // Always clear local storage regardless of server response
            this.clearAuth();
        });
    },

    /**
     * Update navbar UI based on authentication status
     */
    updateNavbar(userInfo) {
        const authSection = document.querySelector('.auth-section');
        if (!authSection) return;

        const notLoggedIn = authSection.querySelector('.auth-not-logined');
        const loggedIn = authSection.querySelector('.auth-logged-in');

        if (userInfo && this.isAuthenticated()) {
            // User is authenticated
            notLoggedIn.style.display = 'none';
            loggedIn.style.display = 'flex';
            
            // Update user info in dropdown
            const userInfo_div = loggedIn.querySelector('.user-info');
            const username_span = userInfo_div.querySelector('.username');
            const email_span = userInfo_div.querySelector('.email');
            
            if (username_span) username_span.textContent = userInfo.username || '';
            if (email_span) email_span.textContent = userInfo.email || '';
            userInfo_div.style.display = 'flex';
            
            // Update avatar
            const avatar = loggedIn.querySelector('.avatar');
            if (avatar) {
                avatar.src = `https://api.dicebear.com/7.x/avataaars/svg?seed=${userInfo.username || 'user'}`;
            }
            
            // Add click handler for profile dropdown toggle
            if (!avatar.__clickHandlerAdded) {
                avatar.addEventListener('click', function(e) {
                    e.stopPropagation();
                    const menu = loggedIn.querySelector('.dropdown-menu');
                    menu.classList.toggle('active');
                });
                avatar.__clickHandlerAdded = true;
            }

            // Reload feather icons
            if (feather) feather.replace();
        } else {
            // User is not authenticated
            notLoggedIn.style.display = 'flex';
            loggedIn.style.display = 'none';

            // Reload feather icons
            if (feather) feather.replace();
        }
    },

    /**
     * Handle logout with confirmation
     */
    handleLogout(event) {
        event.preventDefault();
        
        // Show confirmation dialog
        if (confirm('Are you sure you want to logout?')) {
            this.logout();
            
            // Redirect after logout (with delay to allow server call)
            setTimeout(() => {
                window.location.href = '/';
            }, 500);
        }
    },

    /**
     * Add JWT token to API request headers
     */
    getAuthHeaders() {
        const token = this.getToken();
        const headers = {
            'Content-Type': 'application/json',
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    },

    /**
     * Initialize on page load
     */
    init() {
        // Check if user is logged in and update navbar
        if (this.isAuthenticated()) {
            const user = this.getUser();
            this.updateNavbar(user);
        }
        
        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            const userProfile = document.querySelector('.user-profile');
            if (userProfile && !userProfile.contains(e.target)) {
                const menu = userProfile?.querySelector('.dropdown-menu');
                if (menu) {
                    menu.classList.remove('active');
                }
            }
        });
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    AuthManager.init();
});
