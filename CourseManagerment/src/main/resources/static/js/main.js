/**
 * COURSE MANAGER - Main JavaScript
 * Handles interactive features: mobile menu, dropdowns, smooth scrolling, etc.
 * Non-conflicting generic UI utilities only.
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

    menuToggle.addEventListener('click', (e) => {
        e.stopPropagation();
        navMenu.classList.toggle('active');
        menuToggle.classList.toggle('active');
    });

    navMenu.querySelectorAll('a').forEach(item => {
        item.addEventListener('click', () => {
            navMenu.classList.remove('active');
            menuToggle.classList.remove('active');
        });
    });

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

    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            const query = searchInput.value.trim();
            if (query) {
                window.location.href = `/courses?search=${encodeURIComponent(query)}`;
            }
        }
    });

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

    document.querySelectorAll(
        '.course-card, .category-card, .feature-card, .section-header'
    ).forEach(el => {
        observer.observe(el);
    });
}

/**
 * Smooth Scroll Helper
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
 * Initialize on Page Load
 */
window.addEventListener('load', () => {
    console.log('Page loaded and fully interactive');
});

// Expose non-conflicting helpers on window
window.CourseManagerUI = {
    smoothScroll,
    formatPrice,
    debounce
};
