package com._6.CourseManagerment.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * Custom authentication details that includes userId extracted from JWT.
 */
public class AuthenticationDetails extends WebAuthenticationDetails {

    private final Long userId;

    public AuthenticationDetails(HttpServletRequest request, Long userId) {
        super(request);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
