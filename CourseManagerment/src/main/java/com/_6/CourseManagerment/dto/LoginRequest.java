package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login request
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Email or username is required")
    private String username; // Can be either email or username
    
    @NotBlank(message = "Password is required")
    private String password;
}
