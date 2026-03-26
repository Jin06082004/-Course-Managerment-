package com._6.CourseManagerment.util;

import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for security-related operations
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    /**
     * Get current user's username from authentication
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String username && !"anonymousUser".equals(username)) {
                return username;
            }
        }

        return null;
    }

    /**
     * Resolve current user ID by username from authentication context
     */
    public Long getCurrentUserId() {
        String username = getCurrentUsername();
        if (username == null) {
            throw new IllegalStateException("Unable to retrieve current user from authentication");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current authenticated user not found: " + username));

        return user.getId();
    }

    /**
     * Check if current user is authenticated
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Check if current user has ADMIN role
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
}
