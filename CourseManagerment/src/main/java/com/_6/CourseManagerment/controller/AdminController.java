package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.UserDto;
import com._6.CourseManagerment.entity.Role;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.RoleRepository;
import com._6.CourseManagerment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Controller - Handles administrative operations
 * Only users with ADMIN role can access these endpoints
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    /**
     * Get all users with pagination and filtering
     * Supports filtering by role and searching by username/email/fullName
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users;
            
            // If search is provided, search by username/email/fullName
            if (search != null && !search.isEmpty()) {
                users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                        search, search, search, pageable);
            } 
            // If role is provided, filter by role
            else if (role != null && !role.isEmpty()) {
                Role roleEntity = roleRepository.findByName(role)
                        .orElse(null);
                if (roleEntity == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Role not found: " + role));
                }
                users = userRepository.findByRole(roleEntity, pageable);
            } 
            // Otherwise get all users
            else {
                users = userRepository.findAll(pageable);
            }
            
            Page<UserDto> userDtos = users.map(this::convertToDto);
            return ResponseEntity.ok(userDtos);
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(convertToDto(user));
        } catch (Exception e) {
            log.error("Error fetching user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update user information
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            User user = userRepository.findById(id)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Update fields
            if (updates.containsKey("fullName")) {
                user.setFullName((String) updates.get("fullName"));
            }
            if (updates.containsKey("status")) {
                user.setStatus((String) updates.get("status"));
            }
            if (updates.containsKey("email")) {
                user.setEmail((String) updates.get("email"));
            }
            
            userRepository.save(user);
            return ResponseEntity.ok(convertToDto(user));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Assign role to user (replaces current role)
     */
    @PostMapping("/users/{id}/roles")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findById(id)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            String roleName = request.get("roleName");
            Role role = roleRepository.findByName(roleName)
                    .orElse(null);
            if (role == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Role not found: " + roleName));
            }
            
            // Replace the user's current role with the new one
            user.setRole(role);
            
            userRepository.save(user);
            log.info("Role {} assigned to user {}", roleName, user.getUsername());
            
            return ResponseEntity.ok(convertToDto(user));
        } catch (Exception e) {
            log.error("Error assigning role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Remove role from user (not applicable for single-role system, returns error)
     */
    @DeleteMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<?> removeRoleFromUser(
            @PathVariable Long id,
            @PathVariable String roleName) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Cannot remove role in single-role system. Use POST endpoint to replace role."));
    }
    
    /**
     * Lock/unlock user account
     */
    @PostMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findById(id)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            String status = request.get("status"); // ACTIVE or LOCKED
            if (!status.equals("ACTIVE") && !status.equals("LOCKED")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status. Must be ACTIVE or LOCKED"));
            }
            
            user.setStatus(status);
            userRepository.save(user);
            return ResponseEntity.ok(convertToDto(user));
        } catch (Exception e) {
            log.error("Error updating user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Delete user
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            userRepository.delete(user);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get role statistics
     */
    @GetMapping("/statistics/roles")
    public ResponseEntity<?> getRoleStatistics() {
        try {
            List<Role> roles = roleRepository.findAll();
            Map<String, Object> stats = new HashMap<>();
            
            for (Role role : roles) {
                long count = userRepository.countByRole(role);
                stats.put(role.getName(), count);
            }
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching role statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Helper method to convert User to UserDto
     */
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRole(user.getRole().getName());
        return dto;
    }
}
