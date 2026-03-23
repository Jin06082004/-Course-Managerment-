package com._6.CourseManagerment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter - Validates JWT token in request header
 * Intercepts requests and extracts JWT token from Authorization header
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                List<String> roles = jwtTokenProvider.getRolesFromToken(jwt);
                
                // Convert role names to GrantedAuthority
                List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> {
                        // Ensure role starts with "ROLE_" for Spring Security
                        if (!role.startsWith("ROLE_")) {
                            return new SimpleGrantedAuthority("ROLE_" + role);
                        }
                        return new SimpleGrantedAuthority(role);
                    })
                    .collect(Collectors.toList());
                
                // Get userId from token claims
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

                authentication.setDetails(new AuthenticationDetails(request, userId));

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Set user authentication for user: {}", username);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
