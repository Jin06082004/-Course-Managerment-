package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for authentication response (after login or register success)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    
    private String message;
    
    private String token;
    
    private String refreshToken;
    
    private UserDto user;
    
    public AuthResponse(String message, String token, UserDto user) {
        this.message = message;
        this.token = token;
        this.user = user;
    }
    
    /**
     * Simple DTO to expose user info (without sensitive data)
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDto {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String avatar;
        private String status;
        private LocalDateTime createdAt;
        private Set<String> roles; // Role names
    }
}
