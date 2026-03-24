package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.entity.Role;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.RoleRepository;
import com._6.CourseManagerment.repository.UserRepository;
import com._6.CourseManagerment.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin Setup Controller - For initial admin role assignment and registration
 */
@RestController
@RequestMapping("/api/setup")
@Slf4j
public class AdminSetupController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user and assign ADMIN role in one step.
     * Use this endpoint to create the first admin account.
     */
    @PostMapping("/register-admin")
    @Transactional
    public ResponseEntity<?> registerAdmin(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            String fullName = request.get("fullName");

            if (username == null || username.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            if (password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }

            if (userRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }
            if (userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }

            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            Role studentRole = roleRepository.findByName("STUDENT").orElse(null);
            Role defaultRole = adminRole != null ? adminRole : studentRole;

            if (defaultRole == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "No roles found in database"));
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName != null ? fullName : username);
            user.setStatus("ACTIVE");
            user.setProvider("LOCAL");
            user.setRole(defaultRole);

            User savedUser = userRepository.save(user);
            log.info("Admin user registered: {}", savedUser.getUsername());

            // Generate token with userId
            String token = jwtTokenProvider.generateToken(
                org.springframework.security.core.userdetails.User.builder()
                    .username(savedUser.getUsername())
                    .password(savedUser.getPassword())
                    .authorities("ROLE_" + defaultRole.getName())
                    .build(),
                savedUser.getId()
            );

            return ResponseEntity.ok(java.util.Map.of(
                "message", "Admin user created successfully",
                "token", token,
                "user", java.util.Map.of(
                    "id", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "email", savedUser.getEmail(),
                    "fullName", savedUser.getFullName(),
                    "role", savedUser.getRole().getName()
                )
            ));
        } catch (Exception e) {
            log.error("Error registering admin", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Assign ROLE_ADMIN to a user by username.
     * Uses native query to avoid JPA proxy loading issues with invalid role_id.
     */
    @PostMapping("/assign-admin")
    @Transactional
    public ResponseEntity<?> assignAdminRole(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            if (username == null || username.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }

            // Use native query to get user data directly without JPA proxy loading for role
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            if (adminRole == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "ADMIN role not found"));
            }

            // Fix user's role_id directly via native update to avoid proxy issues
            userRepository.updateUserRole(user.getId(), adminRole.getId());
            user.setRole(adminRole);

            log.info("ADMIN role assigned to user: {} (role_id set to {})", username, adminRole.getId());
            return ResponseEntity.ok(Map.of(
                "message", "ADMIN role assigned successfully",
                "username", user.getUsername(),
                "role", "ADMIN"
            ));
        } catch (Exception e) {
            log.error("Error assigning admin role", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
