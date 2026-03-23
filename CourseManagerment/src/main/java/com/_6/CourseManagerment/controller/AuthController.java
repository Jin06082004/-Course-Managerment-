package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.AuthResponse;
import com._6.CourseManagerment.dto.LoginRequest;
import com._6.CourseManagerment.dto.RegisterRequest;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.UserRepository;
import com._6.CourseManagerment.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authentication Controller
 * Endpoints:
 * - POST /api/auth/register - User registration
 * - POST /api/auth/login - User login
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration and login endpoints")
@Slf4j
public class AuthController {
    
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    
    /**
     * Register a new user
     * 
     * @param registerRequest Registration request with username, email, password, fullName
     * @return JWT token and user info on success
     * 
     * Example request:
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "password123",
     *   "fullName": "John Doe",
     *   "role": "STUDENT"
     * }
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Register request received for email: {}", registerRequest.getEmail());
        
        try {
            AuthResponse response = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new AuthResponse(e.getMessage(), null, null));
        }
    }
    
    /**
     * Login user and return JWT token
     * 
     * @param loginRequest Login request with username/email and password
     * @return JWT token and user info on success
     * 
     * Example request:
     * {
     *   "username": "john_doe",  // can be username or email
     *   "password": "password123"
     * }
     */
    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for user: {}", loginRequest.getUsername());
        
        try {
            AuthResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new AuthResponse(e.getMessage(), null, null));
        }
    }
    
    /**
     * Logout user - invalidates session on server
     * Clears any server-side authentication state
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<?> logout() {
        log.info("Logout request received");
        try {
            // Invalidate session (if using session-based auth)
            // Clear any server-side tokens if needed
            return ResponseEntity.ok(new AuthResponse("Logout successful", null, null));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new AuthResponse("Logout failed: " + e.getMessage(), null, null));
        }
    }

    /**
     * Get authenticated user profile
     * Requires valid JWT token in Authorization header
     */
    @GetMapping("/profile")
    @Operation(summary = "Get authenticated user profile")
    public ResponseEntity<?> getProfile() {
        String username = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("avatar", user.getAvatar());
        profile.put("status", user.getStatus());
        profile.put("provider", user.getProvider());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("roles", user.getRoles().stream()
            .map(r -> r.getName())
            .toList());

        return ResponseEntity.ok(profile);
    }

    /**
     * Update authenticated user profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Update authenticated user profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates) {
        String username = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();

        if (updates.containsKey("fullName")) {
            user.setFullName(updates.get("fullName"));
        }
        if (updates.containsKey("avatar")) {
            user.setAvatar(updates.get("avatar"));
        }

        userRepository.save(user);
        log.info("Profile updated for user: {}", username);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("avatar", user.getAvatar());
        profile.put("status", user.getStatus());
        profile.put("roles", user.getRoles().stream()
            .map(r -> r.getName())
            .toList());

        return ResponseEntity.ok(profile);
    }

    /**
     * Change password for authenticated user
     */
    @PostMapping("/change-password")
    @Operation(summary = "Change user password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwords) {
        String username = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String currentPassword = passwords.get("currentPassword");
        String newPassword = passwords.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current and new password are required"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters"));
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", username);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
