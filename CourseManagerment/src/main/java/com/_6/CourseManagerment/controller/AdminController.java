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
     * Get all users with pagination
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users;
            
            if (role != null && !role.isEmpty()) {
                // Filter by role if specified
                Role roleEntity = roleRepository.findByName(role)
                        .orElse(null);
                if (roleEntity == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Role not found: " + role));
                }
                users = userRepository.findByRoles(Collections.singleton(roleEntity), pageable);
            } else {
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
     * Assign role to user
     * When assigning ROLE_ADMIN or ROLE_INSTRUCTOR, automatically removes ROLE_STUDENT
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
            
            // Check if user already has this role
            if (user.getRoles().contains(role)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User already has role: " + roleName));
            }
            
            // Add the new role
            user.getRoles().add(role);
            
            // Auto-remove STUDENT when promoting to ADMIN or INSTRUCTOR
            if ((roleName.equals("ADMIN") || roleName.equals("INSTRUCTOR") || 
                    roleName.equals("ROLE_ADMIN") || roleName.equals("ROLE_INSTRUCTOR")) 
                    && user.getRoles().size() > 1) {
                Role studentRole = roleRepository.findByName("STUDENT")
                        .orElse(roleRepository.findByName("ROLE_STUDENT").orElse(null));
                if (studentRole != null && user.getRoles().contains(studentRole)) {
                    user.getRoles().remove(studentRole);
                    log.info("Automatically removed STUDENT role from user {} when assigning {}", 
                            user.getUsername(), roleName);
                }
            }
            
            userRepository.save(user);
            log.info("Role {} assigned to user {} (remaining roles: {})", 
                    roleName, user.getUsername(), 
                    user.getRoles().stream().map(Role::getName).toList());
            
            return ResponseEntity.ok(convertToDto(user));
        } catch (Exception e) {
            log.error("Error assigning role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Remove role from user
     */
    @DeleteMapping("/users/{id}/roles/{roleName}")
    public ResponseEntity<?> removeRoleFromUser(
            @PathVariable Long id,
            @PathVariable String roleName) {
        try {
            User user = userRepository.findById(id)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Role role = roleRepository.findByName(roleName)
                    .orElse(null);
            if (role == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Role not found: " + roleName));
            }
            
            user.getRoles().remove(role);
            userRepository.save(user);
            return ResponseEntity.ok(convertToDto(user));
        } catch (Exception e) {
            log.error("Error removing role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
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
                long count = userRepository.countByRoles(Collections.singleton(role));
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
        dto.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return dto;
    }
}
