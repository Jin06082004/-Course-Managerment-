package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.entity.Role;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.RoleRepository;
import com._6.CourseManagerment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Admin Setup Controller - For initial admin role assignment
 * This is a temporary endpoint for setting up the first admin user
 */
@RestController
@RequestMapping("/api/setup")
@Slf4j
public class AdminSetupController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    /**
     * Assign ROLE_ADMIN to a user by username
     * This endpoint is for initial setup only
     */
    @PostMapping("/assign-admin")
    public ResponseEntity<?> assignAdminRole(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            if (username == null || username.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username is required"));
            }
            
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElse(null);
            if (adminRole == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "ADMIN role not found"));
            }
            
            // Check if already has admin role
            if (user.getRoles().contains(adminRole)) {
                return ResponseEntity.ok(Map.of("message", "User already has ADMIN role"));
            }
            
            // Add ADMIN role
            Set<Role> roles = new HashSet<>(user.getRoles());
            roles.add(adminRole);
            user.setRoles(roles);
            
            userRepository.save(user);
            
            log.info("ADMIN role assigned to user: {}", username);
            return ResponseEntity.ok(Map.of(
                    "message", "ADMIN role assigned successfully",
                    "username", user.getUsername(),
                    "roles", user.getRoles().stream().map(Role::getName).toList()
            ));
        } catch (Exception e) {
            log.error("Error assigning admin role", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
