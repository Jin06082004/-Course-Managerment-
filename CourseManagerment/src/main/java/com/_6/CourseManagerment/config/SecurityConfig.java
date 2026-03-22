package com._6.CourseManagerment.config;

import com._6.CourseManagerment.security.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Spring Security Configuration
 * - JWT-based stateless authentication
 * - CORS support
 * - Role-based authorization
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * Password encoder using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * CORS Configuration
     * Allows requests from your frontend with credentials support
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins (change to your frontend URL in production)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:8080",
            "http://localhost:3000",
            "http://localhost:4200"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Set allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Set allowed headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Expose headers
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        
        // Max age of preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * Authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * Security filter chain configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF since we're using JWT
            .csrf(csrf -> csrf.disable())
            
            // Enable CORS with configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Use stateless session (JWT doesn't require sessions)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure URL security
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - Auth
                .requestMatchers("/api/auth/**").permitAll()

                // Public endpoints - Browsing courses
                .requestMatchers("/api/courses/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                
                // Public pages (HTML từ Thymeleaf — trình duyệt KHÔNG gửi JWT trong localStorage khi gõ URL / F5)
                // Xác thực thật sự vẫn do JWT trên /api/** khi frontend gọi fetch + Authorization
                .requestMatchers(
                        "/",
                        "/home",
                        "/courses",
                        "/courses/**",
                        "/login",
                        "/register",
                        "/my-courses",
                        "/profile",
                        "/settings",
                        "/wishlist",
                        "/error/404",
                        "/error/500",
                        "/error"
                ).permitAll()
                
                // Public assets
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                
                // Public API docs
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                
                // Any other request requires authentication
                .anyRequest().authenticated()
            )
            
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Exception handling
            .exceptionHandling(handler -> handler
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(401, "Unauthorized: " + authException.getMessage());
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendError(403, "Access Denied: " + accessDeniedException.getMessage());
                })
            );
        
        return http.build();
    }
}
