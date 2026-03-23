package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * User Data Transfer Object - For API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatar;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> roles;
}
