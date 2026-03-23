package com._6.CourseManagerment.security;

import com._6.CourseManagerment.entity.User;
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
     * Get current authenticated user's username.
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getName();
    }
    
    /**
     * Get current authenticated User entity from Authentication
     * Note: This is a utility method for extracting user principal from auth
     */
    public static User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        // If principal is a username string, return null (would need repository to look up)
        return null;
    }
}
