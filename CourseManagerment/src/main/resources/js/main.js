/**
 * COURSE MANAGER - Main JavaScript
 * Handles interactive features: mobile menu, dropdowns, smooth scrolling, etc.
 */

document.addEventListener('DOMContentLoaded', () => {
    initializeNavbar();
    initializeSearch();
    initializeDropdowns();
    initializeScrollAnimations();
});

/**
 * Initialize Navbar Mobile Menu Toggle
 */
function initializeNavbar() {
    const menuToggle = document.getElementById('menuToggle');
    const navMenu = document.getElementById('navMenu');
    
    if (!menuToggle || !navMenu) return;
    
    // Toggle menu on hamburger click
    menuToggle.addEventListener('click', (e) => {
        e.stopPropagation();
        navMenu.classList.toggle('active');
        menuToggle.classList.toggle('active');
    });
    
    // Close menu when clicking on a nav item
    navMenu.querySelectorAll('a').forEach(item => {
        item.addEventListener('click', () => {
            navMenu.classList.remove('active');
            menuToggle.classList.remove('active');
        });
    });
    
    // Close menu when clicking outside
    document.addEventListener('click', (e) => {
        if (!e.target.closest('.navbar')) {
            navMenu.classList.remove('active');
            menuToggle.classList.remove('active');
        }
    });
}

/**
 * Initialize Search Functionality
 */
function initializeSearch() {
    const searchInput = document.getElementById('navSearch');
    const searchBtn = document.querySelector('.search-btn');
    
    if (!searchInput) return;
    
    // Handle search on enter
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            const query = searchInput.value.trim();
            if (query) {
                window.location.href = `/courses?search=${encodeURIComponent(query)}`;
            }
        }
    });
    
    // Handle search button click
    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            const query = searchInput.value.trim();
            if (query) {
                window.location.href = `/courses?search=${encodeURIComponent(query)}`;
            }
        });
    }
}

/**
 * Initialize Dropdown Menus (e.g., User Profile)
 */
function initializeDropdowns() {
    const userProfiles = document.querySelectorAll('.user-profile');
    
    userProfiles.forEach(profile => {
        const avatar = profile.querySelector('.avatar');
        const dropdown = profile.querySelector('.dropdown-menu');
        
        if (!avatar || !dropdown) return;
        
        avatar.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
        });
        
        // Close dropdown when clicking outside
        document.addEventListener('click', () => {
            dropdown.style.display = 'none';
        });
    });
}

/**
 * Initialize Scroll Animations
 * Fade in elements as they come into view
 */
function initializeScrollAnimations() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('fade-in');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);
    
    // Observe course cards and feature cards
    document.querySelectorAll(
        '.course-card, .category-card, .feature-card, .section-header'
    ).forEach(el => {
        observer.observe(el);
    });
}

/**
 * Smooth Scroll Helper
 * Usage: smoothScroll('#section-id')
 */
function smoothScroll(target) {
    const element = document.querySelector(target);
    if (element) {
        element.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    }
}

/**
 * Add to Wishlist Handler
 */
document.addEventListener('click', (e) => {
    if (e.target.closest('.btn-icon') && e.target.closest('.course-card')) {
        const button = e.target.closest('.btn-icon');
        const courseId = button.closest('.course-card').dataset.courseId;
        
        if (courseId) {
            toggleWishlist(courseId, button);
        }
    }
});

function toggleWishlist(courseId, button) {
    // TODO: Add API call to toggle wishlist
    button.classList.toggle('active');
    console.log('Toggled wishlist for course:', courseId);
}

/**
 * Format Price Helper
 */
function formatPrice(price) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 2
    }).format(price);
}

/**
 * Get Authentication Token from localStorage
 */
function getAuthToken() {
    return localStorage.getItem('auth_token');
}

/**
 * Set Authentication Token to localStorage
 */
function setAuthToken(token) {
    localStorage.setItem('auth_token', token);
}

/**
 * Clear Authentication Token from localStorage
 */
function clearAuthToken() {
    localStorage.removeItem('auth_token');
}

/**
 * Make Authenticated API Call
 */
async function apiCall(endpoint, options = {}) {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    try {
        const response = await fetch(endpoint, {
            ...options,
            headers
        });
        
        if (response.status === 401) {
            // Token expired or invalid
            clearAuthToken();
            window.location.href = '/login';
            return null;
        }
        
        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        return null;
    }
}

/**
 * Debounce Helper for Search Input
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Show Toast Notification
 */
function showToast(message, type = 'info') {
    // TODO: Create toast component
    console.log(`[${type.toUpperCase()}] ${message}`);
}

/**
 * Initialize on Page Load
 */
window.addEventListener('load', () => {
    // Add any animations or effects that depend on full page load
    console.log('Page loaded and fully interactive');
});

export {
    smoothScroll,
    formatPrice,
    getAuthToken,
    setAuthToken,
    clearAuthToken,
    apiCall,
    debounce,
    showToast
};
