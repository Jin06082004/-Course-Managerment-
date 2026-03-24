package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.entity.Role;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.RoleRepository;
import com._6.CourseManagerment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database Reset and User Setup Controller
 */
@RestController
@RequestMapping("/api/setup")
@Slf4j
public class DatabaseResetController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Reset database - delete all users and create demo users
     */
    @PostMapping("/reset-users")
    public ResponseEntity<?> resetUsers() {
        try {
            // Delete all users
            long deletedCount = userRepository.count();
            userRepository.deleteAll();
            log.info("Deleted {} users from database", deletedCount);
            
            // Ensure roles exist
            Map<String, Role> roles = new HashMap<>();
            ensureRoleExists("ADMIN", roles);
            ensureRoleExists("INSTRUCTOR", roles);
            ensureRoleExists("STUDENT", roles);
            
            // Create demo users
            List<Map<String, Object>> createdUsers = new ArrayList<>();
            
            // Admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setStatus("ACTIVE");
            admin.setProvider("LOCAL");
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setRole(roles.get("ADMIN"));
            User savedAdmin = userRepository.save(admin);
            createdUsers.add(Map.of(
                    "id", savedAdmin.getId(),
                    "username", savedAdmin.getUsername(),
                    "email", savedAdmin.getEmail(),
                    "role", savedAdmin.getRole().getName()
            ));
            log.info("Created admin user: {}", savedAdmin.getUsername());
            
            // Instructor user
            User instructor = new User();
            instructor.setUsername("instructor");
            instructor.setEmail("instructor@example.com");
            instructor.setPassword(passwordEncoder.encode("instructor123"));
            instructor.setFullName("John Instructor");
            instructor.setStatus("ACTIVE");
            instructor.setProvider("LOCAL");
            instructor.setCreatedAt(LocalDateTime.now());
            instructor.setUpdatedAt(LocalDateTime.now());
            instructor.setRole(roles.get("INSTRUCTOR"));
            User savedInstructor = userRepository.save(instructor);
            createdUsers.add(Map.of(
                    "id", savedInstructor.getId(),
                    "username", savedInstructor.getUsername(),
                    "email", savedInstructor.getEmail(),
                    "role", savedInstructor.getRole().getName()
            ));
            log.info("Created instructor user: {}", savedInstructor.getUsername());
            
            // Student user 1
            User student1 = new User();
            student1.setUsername("student1");
            student1.setEmail("student1@example.com");
            student1.setPassword(passwordEncoder.encode("student123"));
            student1.setFullName("Alice Student");
            student1.setStatus("ACTIVE");
            student1.setProvider("LOCAL");
            student1.setCreatedAt(LocalDateTime.now());
            student1.setUpdatedAt(LocalDateTime.now());
            student1.setRole(roles.get("STUDENT"));
            User savedStudent1 = userRepository.save(student1);
            createdUsers.add(Map.of(
                    "id", savedStudent1.getId(),
                    "username", savedStudent1.getUsername(),
                    "email", savedStudent1.getEmail(),
                    "role", savedStudent1.getRole().getName()
            ));
            log.info("Created student user: {}", savedStudent1.getUsername());
            
            // Student user 2
            User student2 = new User();
            student2.setUsername("student2");
            student2.setEmail("student2@example.com");
            student2.setPassword(passwordEncoder.encode("student123"));
            student2.setFullName("Bob Student");
            student2.setStatus("ACTIVE");
            student2.setProvider("LOCAL");
            student2.setCreatedAt(LocalDateTime.now());
            student2.setUpdatedAt(LocalDateTime.now());
            student2.setRole(roles.get("STUDENT"));
            User savedStudent2 = userRepository.save(student2);
            createdUsers.add(Map.of(
                    "id", savedStudent2.getId(),
                    "username", savedStudent2.getUsername(),
                    "email", savedStudent2.getEmail(),
                    "role", savedStudent2.getRole().getName()
            ));
            log.info("Created student user: {}", savedStudent2.getUsername());
            
            return ResponseEntity.ok(Map.of(
                    "message", "Database reset successfully",
                    "deletedUsers", deletedCount,
                    "createdUsers", createdUsers
            ));
        } catch (Exception e) {
            log.error("Error resetting database", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Ensure a role exists, create if it doesn't
     */
    private void ensureRoleExists(String roleName, Map<String, Role> roles) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role(null, roleName);
                    return roleRepository.save(newRole);
                });
        roles.put(roleName, role);
        log.info("Role {} ensured in database", roleName);
    }
    
    /**
     * Get all users (for testing)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> userList = new ArrayList<>();
            
            for (User user : users) {
                userList.add(Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "status", user.getStatus(),
                        "role", user.getRole().getName()
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                    "totalCount", userList.size(),
                    "users", userList
            ));
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
