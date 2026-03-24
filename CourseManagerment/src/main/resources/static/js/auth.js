/**
 * Authentication Management
 * Handles login/registration and navbar UI updates
 */

/**
 * Expose on window so all inline scripts and Thymeleaf pages can access it reliably.
 * (Top-level `const` is not always visible as `window.AuthManager` in every browser context.)
 */
window.AuthManager = {
    TOKEN_KEY: 'authToken',
    USER_KEY: 'userInfo',

    /**
     * Decode JWT token and extract claims
     * JWT format: header.payload.signature
     */
    decodeToken(token) {
        try {
            const parts = token.split('.');
            if (parts.length !== 3) return null;
            
            const decoded = atob(parts[1]);
            return JSON.parse(decoded);
        } catch (error) {
            console.error('Error decoding token:', error);
            return null;
        }
    },

    /**
     * Get role from JWT token claims
     * After single-role migration, extract the single role from the roles array
     */
    getRoleFromToken(token) {
        const claims = this.decodeToken(token);
        if (!claims) return null;
        
        // JWT contains "roles" array - get the first (and only) role
        const rolesArray = claims.roles || [];
        if (rolesArray.length === 0) return null;
        
        // Remove "ROLE_" prefix if present
        let role = rolesArray[0];
        if (role && role.startsWith('ROLE_')) {
            role = role.replace('ROLE_', '');
        }
        
        console.log('Role extracted from JWT token claims:', role);
        return role;
    },

    /**
     * Save token and user info to localStorage
     */
    saveAuth(token, userInfo) {
        // If role is not in userInfo, try to extract from JWT token
        if (!userInfo.role) {
            const roleFromToken = this.getRoleFromToken(token);
            if (roleFromToken) {
                userInfo.role = roleFromToken;
                console.log('Extracted role from JWT token and updated userInfo:', roleFromToken);
            }
        }
        
        console.log('Saving auth with userInfo:', userInfo);
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
        let userInfo = user ? JSON.parse(user) : null;
        
        // If user exists but role is missing, try to extract from JWT token
        if (userInfo && !userInfo.role) {
            const token = this.getToken();
            if (token) {
                const roleFromToken = this.getRoleFromToken(token);
                if (roleFromToken) {
                    userInfo.role = roleFromToken;
                    // Update localStorage with extracted role
                    localStorage.setItem(this.USER_KEY, JSON.stringify(userInfo));
                }
            }
        }
        
        return userInfo;
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
            if (!userInfo_div) return;

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
     * Check if user has a specific role
     */
    hasRole(role) {
        const user = this.getUser();
        if (!user) {
            console.log('No user info found');
            return false;
        }
        
        const userRole = user.role || '';
        console.log('Checking role:', role, 'User role:', userRole);
        
        // Check both with and without ROLE_ prefix
        const roleToCheck = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
        const hasRole = userRole === role || userRole === roleToCheck || userRole === role.replace('ROLE_', '');
        console.log('Role check result:', hasRole);
        return hasRole;
    },

    /**
     * Require specific role, redirect to login if not authenticated
     * or redirect to home if authenticated but doesn't have role
     */
    requireRole(role, redirectUrl = '/login') {
        if (!this.isAuthenticated()) {
            window.location.href = redirectUrl;
            return false;
        }
        
        if (!this.hasRole(role)) {
            // User is authenticated but doesn't have required role
            alert(`You do not have ${role} permissions.`);
            window.location.href = '/';
            return false;
        }
        
        return true;
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

// Alias for code that references `AuthManager` without `window.`
const AuthManager = window.AuthManager;

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    window.AuthManager.init();
});
