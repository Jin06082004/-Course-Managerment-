package com._6.CourseManagerment.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper class to extract user ID from JWT token stored in SecurityContext.
 * The userId is embedded in the JWT token claims during generation.
 */
public class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Get current authenticated user's ID.
     * Returns null if not authenticated or if token doesn't contain userId.
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object details = auth.getDetails();
        if (details instanceof AuthenticationDetails) {
            return ((AuthenticationDetails) details).getUserId();
        }
        return null;
    }

    /**
     * Get current authenticated username.
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getName();
    }
}
