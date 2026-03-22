package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.AuthResponse;
import com._6.CourseManagerment.dto.AuthResponse.UserDto;
import com._6.CourseManagerment.dto.LoginRequest;
import com._6.CourseManagerment.dto.RegisterRequest;
import com._6.CourseManagerment.entity.Role;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.RoleRepository;
import com._6.CourseManagerment.repository.UserRepository;
import com._6.CourseManagerment.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Service - Handles user registration and login
 * Includes user creation, role assignment, JWT token generation
 */
@Service
@Transactional
@Slf4j
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Register a new user
     * - Validate registration data
     * - Create user with STUDENT role (default)
     * - Encode password
     * - Send registration email
     * - Return JWT token
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Registering new user: {}", registerRequest.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Username already exists: {}", registerRequest.getUsername());
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Email already exists: {}", registerRequest.getEmail());
            throw new RuntimeException("Email already exists");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());
        user.setStatus("ACTIVE");
        user.setProvider("LOCAL");
        
        // Assign role (default: STUDENT)
        String roleName = registerRequest.getRole() != null ? registerRequest.getRole() : "STUDENT";
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        
        // Save user to database
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());
        
        // Send registration confirmation email (async would be better)
        try {
            emailService.sendRegistrationSuccessEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            log.error("Failed to send registration email: {}", e.getMessage());
        }
        
        // Generate JWT token using saved user
        String token = generateTokenForUser(user);
        
        // Build response
        UserDto userDto = convertToUserDto(savedUser);
        return new AuthResponse(
            "User registered successfully",
            token,
            userDto
        );
    }
    
    /**
     * Login user
     * - Authenticate using username/email and password
     * - Generate JWT token
     * - Return user info and token
     */
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("User login attempt: {}", loginRequest.getUsername());
        
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            
            // Get user details from authentication
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Fetch full user object
            User user = userRepository.findByEmailOrUsername(
                loginRequest.getUsername(),
                loginRequest.getUsername()
            ).orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if account is locked
            if (user.getStatus().equals("LOCKED")) {
                log.warn("Account locked: {}", user.getUsername());
                throw new RuntimeException("Account is locked. Please contact support.");
            }
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(userDetails);
            
            log.info("User logged in successfully: {}", user.getUsername());
            
            // Build response
            UserDto userDto = convertToUserDto(user);
            return new AuthResponse(
                "Login successful",
                token,
                userDto
            );
            
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", loginRequest.getUsername());
            throw new RuntimeException("Invalid username or password");
        }
    }
    
    /**
     * Generate JWT token for a user
     */
    private String generateTokenForUser(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles().stream()
                .map(role -> {
                    String roleName = role.getName();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    return new org.springframework.security.core.authority.SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList()))
            .build();
        
        return jwtTokenProvider.generateToken(userDetails);
    }
    
    /**
     * Convert User entity to UserDto (without sensitive data)
     */
    private UserDto convertToUserDto(User user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getAvatar(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet())
        );
    }
}
