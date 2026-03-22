/**
 * CourseManager API Service
 * Centralized API calls for courses, categories, and enrollments
 * Uses AuthManager for JWT token management
 */

const API_BASE = '/api';

const ApiService = {
    /**
     * Generic fetch wrapper with auth headers
     */
    async fetch(endpoint, options = {}) {
        const defaultHeaders = {
            'Content-Type': 'application/json',
        };

        const token = (typeof window !== 'undefined' && window.AuthManager && typeof window.AuthManager.getToken === 'function')
            ? window.AuthManager.getToken()
            : null;
        if (token) {
            defaultHeaders['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers: {
                ...defaultHeaders,
                ...(options.headers || {}),
            },
        };

        const response = await fetch(`${API_BASE}${endpoint}`, config);

        // Handle non-JSON responses
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }
        return await response.text();
    },

    // =====================
    // COURSES
    // =====================

    /**
     * Get all courses with pagination
     */
    async getCourses({ page = 0, size = 12, sortBy = 'id', direction = 'DESC' } = {}) {
        return this.fetch(`/courses?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`);
    },

    /**
     * Search courses with filters
     */
    async searchCourses({ title = '', categoryId = null, level = '', page = 0, size = 12 } = {}) {
        let params = new URLSearchParams({ page, size });
        if (title) params.append('title', title);
        if (categoryId) params.append('categoryId', categoryId);
        if (level) params.append('level', level);
        return this.fetch(`/courses/search?${params.toString()}`);
    },

    /**
     * Get featured courses
     */
    async getFeaturedCourses({ page = 0, size = 4 } = {}) {
        return this.fetch(`/courses/featured?page=${page}&size=${size}`);
    },

    /**
     * Get course by ID
     */
    async getCourse(courseId) {
        return this.fetch(`/courses/${courseId}`);
    },

    /**
     * Get courses by category
     */
    async getCoursesByCategory(categoryId, { page = 0, size = 12 } = {}) {
        return this.fetch(`/courses/category/${categoryId}?page=${page}&size=${size}`);
    },

    /**
     * Get courses by instructor
     */
    async getCoursesByInstructor(instructorId, { page = 0, size = 12 } = {}) {
        return this.fetch(`/courses/instructor/${instructorId}?page=${page}&size=${size}`);
    },

    // =====================
    // CATEGORIES
    // =====================

    /**
     * Get all categories
     */
    async getCategories() {
        return this.fetch('/categories');
    },

    /**
     * Get category by ID
     */
    async getCategory(categoryId) {
        return this.fetch(`/categories/${categoryId}`);
    },

    // =====================
    // ENROLLMENTS
    // =====================

    /**
     * Get my enrollments
     */
    async getMyEnrollments({ page = 0, size = 20, sortBy = 'enrollmentDate', direction = 'DESC' } = {}) {
        return this.fetch(`/enrollments/my-courses?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`);
    },

    /**
     * Get active enrollments
     */
    async getActiveEnrollments({ page = 0, size = 20 } = {}) {
        return this.fetch(`/enrollments/active?page=${page}&size=${size}`);
    },

    /**
     * Get completed enrollments
     */
    async getCompletedEnrollments({ page = 0, size = 20 } = {}) {
        return this.fetch(`/enrollments/completed?page=${page}&size=${size}`);
    },

    /**
     * Check if enrolled in a course
     */
    async checkEnrollment(courseId) {
        return this.fetch(`/enrollments/check/${courseId}`);
    },

    /**
     * Enroll in a course
     */
    async enrollInCourse(courseId) {
        return this.fetch(`/enrollments/enroll/${courseId}`, { method: 'POST' });
    },

    /**
     * Unenroll from a course
     */
    async unenrollFromCourse(courseId) {
        return this.fetch(`/enrollments/${courseId}/unenroll`, { method: 'DELETE' });
    },

    /**
     * Update enrollment progress
     */
    async updateProgress(enrollmentId, progressPercentage) {
        return this.fetch(`/enrollments/${enrollmentId}/progress?progressPercentage=${progressPercentage}`, {
            method: 'PUT',
        });
    },

    /**
     * Complete an enrollment
     */
    async completeEnrollment(enrollmentId) {
        return this.fetch(`/enrollments/${enrollmentId}/complete`, { method: 'PUT' });
    },

    // =====================
    // AUTH HELPERS
    // =====================

    /**
     * Get current user profile
     */
    async getProfile() {
        return this.fetch('/auth/profile');
    },

    // =====================
    // WISHLIST
    // =====================

    /**
     * Get user's wishlist
     */
    async getWishlist({ page = 0, size = 20 } = {}) {
        return this.fetch(`/wishlist?page=${page}&size=${size}`);
    },

    /**
     * Toggle course in wishlist (add or remove)
     * Returns { inWishlist: boolean }
     */
    async toggleWishlist(courseId) {
        return this.fetch(`/wishlist/toggle/${courseId}`, { method: 'POST' });
    },

    /**
     * Add course to wishlist
     */
    async addToWishlist(courseId) {
        return this.fetch(`/wishlist/${courseId}`, { method: 'POST' });
    },

    /**
     * Remove course from wishlist
     */
    async removeFromWishlist(courseId) {
        return this.fetch(`/wishlist/${courseId}`, { method: 'DELETE' });
    },

    /**
     * Check if course is in wishlist
     */
    async checkWishlist(courseId) {
        return this.fetch(`/wishlist/check/${courseId}`);
    },

    // =====================
    // WISHLIST STATE CACHE
    // =====================
    // Lightweight in-memory cache for wishlist state (per page load)
    _wishlistCache: new Set(),

    /**
     * Mark a course as in-wishlist in cache
     */
    setWishlistCached(courseId, value) {
        if (value) this._wishlistCache.add(String(courseId));
        else this._wishlistCache.delete(String(courseId));
    },

    /**
     * Check if course is cached as in-wishlist
     */
    isWishlistCached(courseId) {
        return this._wishlistCache.has(String(courseId));
    },
};

// =====================
// UI HELPER FUNCTIONS
// =====================

/**
 * Render a course card from API data
 */
function renderCourseCard(course) {
    const price = course.price ? `$${parseFloat(course.price).toFixed(2)}` : 'Free';
    const level = course.level || 'Beginner';
    const rating = course.rating ? course.rating.toFixed(1) : '0.0';
    const students = course.studentCount || 0;
    const duration = course.duration ? `${course.duration}h` : 'N/A';
    const thumbnail = course.thumbnailUrl || 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=225&fit=crop';
    const instructor = course.instructorName || 'Unknown Instructor';

    return `
        <div class="course-card" data-course-id="${course.id}">
            <div class="card-image-wrapper">
                <img src="${thumbnail}" alt="${course.title}" class="card-image" loading="lazy"
                     onerror="this.src='https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=400&h=225&fit=crop'">
                <div class="card-overlay">
                    <a href="/courses/${course.id}" class="card-view-btn">View Course</a>
                </div>
                <span class="course-badge">${level}</span>
            </div>
            <div class="card-content">
                <h3 class="course-title">${course.title}</h3>
                <p class="instructor-name">By ${instructor}</p>
                <div class="course-meta">
                    <span class="rating">
                        <i data-feather="star" class="star-icon"></i>
                        ${rating} (${students})
                    </span>
                    <span class="duration">${duration}</span>
                </div>
                <div class="card-footer">
                    <span class="price">${price}</span>
                    <button class="btn btn-icon wishlist-btn ${ApiService.isWishlistCached(course.id) ? 'active' : ''}"
                            data-course-id="${course.id}"
                            title="${ApiService.isWishlistCached(course.id) ? 'Remove from Wishlist' : 'Add to Wishlist'}"
                            onclick="handleWishlistToggle(this, ${course.id}, event)">
                        <i data-feather="${ApiService.isWishlistCached(course.id) ? 'heart' : 'heart'}"></i>
                    </button>
                </div>
            </div>
        </div>
    `;
}

/**
 * Render skeleton loading card
 */
function renderCourseCardSkeleton() {
    return `
        <div class="course-card skeleton">
            <div class="card-image-wrapper skeleton-image"></div>
            <div class="card-content">
                <div class="skeleton-text skeleton-title"></div>
                <div class="skeleton-text skeleton-subtitle"></div>
                <div class="skeleton-text skeleton-meta"></div>
                <div class="skeleton-text skeleton-footer"></div>
            </div>
        </div>
    `;
}

/**
 * Show loading state in a container
 */
function showLoading(containerId, count = 6) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = Array(count).fill(renderCourseCardSkeleton()).join('');
}

/**
 * Show error state in a container
 */
function showError(containerId, message = 'Failed to load courses. Please try again.') {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: var(--spacing-3xl);">
            <i data-feather="alert-circle" style="width: 48px; height: 48px; color: var(--accent-primary); margin-bottom: var(--spacing-lg); display: block; margin-left: auto; margin-right: auto;"></i>
            <h3 style="color: var(--text-primary); margin-bottom: var(--spacing-sm);">Oops! Something went wrong</h3>
            <p style="color: var(--text-secondary); margin-bottom: var(--spacing-lg);">${message}</p>
            <button class="btn btn-primary" onclick="location.reload()">
                <i data-feather="refresh-cw"></i>
                Retry
            </button>
        </div>
    `;
    if (typeof feather !== 'undefined') feather.replace();
}

/**
 * Show empty state in a container
 */
function showEmpty(containerId, message = 'No courses found.', actionText = 'Browse All Courses', actionHref = '/courses') {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: var(--spacing-3xl);">
            <i data-feather="book-open" style="width: 48px; height: 48px; color: var(--text-tertiary); margin-bottom: var(--spacing-lg); display: block; margin-left: auto; margin-right: auto;"></i>
            <h3 style="color: var(--text-primary); margin-bottom: var(--spacing-sm);">${message}</h3>
            <p style="color: var(--text-secondary); margin-bottom: var(--spacing-lg);">Try adjusting your filters or search terms.</p>
            <a href="${actionHref}" class="btn btn-primary">
                <i data-feather="arrow-right"></i>
                ${actionText}
            </a>
        </div>
    `;
    if (typeof feather !== 'undefined') feather.replace();
}

/**
 * Format price for display
 */
function formatPrice(price) {
    if (price === null || price === undefined || price === 0) return 'Free';
    return `$${parseFloat(price).toFixed(2)}`;
}

/**
 * Format date string to relative time
 */
function formatRelativeTime(dateString) {
    if (!dateString) return 'Never';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
}

/**
 * Debounce function for search input
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
 * Handle wishlist toggle button click
 * Checks auth first, then toggles via API
 */
function handleWishlistToggle(btn, courseId, event) {
    if (event) event.preventDefault();
    event.stopPropagation();

    const token = window.AuthManager?.getToken?.();
    if (!token) {
        window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
        return;
    }

    const icon = btn.querySelector('i');
    btn.disabled = true;

    ApiService.toggleWishlist(courseId)
        .then(data => {
            ApiService.setWishlistCached(courseId, data.inWishlist);
            btn.classList.toggle('active', data.inWishlist);
            btn.title = data.inWishlist ? 'Remove from Wishlist' : 'Add to Wishlist';
            if (icon) {
                icon.setAttribute('data-feather', data.inWishlist ? 'heart' : 'heart');
                if (typeof feather !== 'undefined') feather.replace();
            }
            if (window.ToastManager) {
                if (data.inWishlist) {
                    window.ToastManager.success('Added to wishlist');
                } else {
                    window.ToastManager.success('Removed from wishlist');
                }
            }
        })
        .catch(err => {
            console.error('Wishlist toggle failed:', err);
            if (window.ToastManager) {
                window.ToastManager.error('Failed to update wishlist');
            }
        })
        .finally(() => {
            btn.disabled = false;
        });
}
