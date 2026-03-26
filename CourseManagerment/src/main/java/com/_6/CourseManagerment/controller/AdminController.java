package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.UserDto;
import com._6.CourseManagerment.entity.Category;
import com._6.CourseManagerment.entity.Role;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.CategoryRepository;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.EnrollmentRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

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

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
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
            Page<Object[]> result;

            if (search != null && !search.isEmpty()) {
                result = userRepository.searchUsersNative(search, pageable);
            } else if (role != null && !role.isEmpty()) {
                Role roleEntity = roleRepository.findByName(role).orElse(null);
                if (roleEntity == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Role not found: " + role));
                }
                result = userRepository.getUsersByRoleNative(roleEntity.getId(), pageable);
            } else {
                result = userRepository.getAllUsersNative(pageable);
            }

            Page<UserDto> userDtos = result.map(this::mapToUserDto);
            return ResponseEntity.ok(userDtos);
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private UserDto mapToUserDto(Object[] row) {
        UserDto dto = new UserDto();
        dto.setId(((Number) row[0]).longValue());
        dto.setUsername((String) row[1]);
        dto.setEmail((String) row[2]);
        dto.setFullName((String) row[3]);
        dto.setAvatar((String) row[4]);
        dto.setStatus((String) row[5]);
        if (row[6] != null) {
            Object createdAt = row[6];
            if (createdAt instanceof java.sql.Timestamp) {
                dto.setCreatedAt(((java.sql.Timestamp) createdAt).toLocalDateTime());
            } else if (createdAt instanceof java.time.LocalDateTime) {
                dto.setCreatedAt((java.time.LocalDateTime) createdAt);
            }
        }
        if (row[7] != null) {
            Object updatedAt = row[7];
            if (updatedAt instanceof java.sql.Timestamp) {
                dto.setUpdatedAt(((java.sql.Timestamp) updatedAt).toLocalDateTime());
            } else if (updatedAt instanceof java.time.LocalDateTime) {
                dto.setUpdatedAt((java.time.LocalDateTime) updatedAt);
            }
        }
        dto.setRole((String) row[8]);
        return dto;
    }

    /**
     * Get user by ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            List<Object[]> result = userRepository.findUserByIdNative(id);
            if (result.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(mapToUserDto(result.get(0)));
        } catch (Exception e) {
            log.error("Error fetching user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update user information (uses native update to avoid broken role FK)
     */
    @Transactional
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            // Check user exists using native query
            if (userRepository.findUserByIdNative(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Build dynamic update SQL
            List<Object> params = new ArrayList<>();
            List<String> setClauses = new ArrayList<>();

            if (updates.containsKey("fullName")) {
                setClauses.add("full_name = ?");
                params.add(updates.get("fullName"));
            }
            if (updates.containsKey("status")) {
                setClauses.add("status = ?");
                params.add(updates.get("status"));
            }
            if (updates.containsKey("email")) {
                setClauses.add("email = ?");
                params.add(updates.get("email"));
            }

            if (setClauses.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No fields to update"));
            }

            params.add(id);
            String sql = "UPDATE users SET " + String.join(", ", setClauses) + " WHERE id = ?";
            jdbcTemplate.update(sql, params.toArray());

            // Return updated user
            List<Object[]> result = userRepository.findUserByIdNative(id);
            if (result.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(mapToUserDto(result.get(0)));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Assign role to user (replaces current role)
     */
    @Transactional
    @PostMapping("/users/{id}/roles")
    public ResponseEntity<?> assignRoleToUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            // Check user exists using native query
            List<Object[]> userResult = userRepository.findUserByIdNative(id);
            if (userResult.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Long roleId = null;
            String roleName = null;
            if (request.containsKey("roleId")) {
                roleId = Long.valueOf(request.get("roleId").toString());
            } else if (request.containsKey("roleName")) {
                roleName = (String) request.get("roleName");
            }

            Long finalRoleId = roleId;
            Role role = null;
            if (roleId != null) {
                role = roleRepository.findById(roleId).orElse(null);
            } else if (roleName != null) {
                role = roleRepository.findByName(roleName).orElse(null);
                if (role != null) finalRoleId = role.getId();
            }

            if (role == null || finalRoleId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Role not found"));
            }

            // Use native update to avoid broken FK issue
            userRepository.updateUserRole(id, finalRoleId);

            // Return updated user using native query
            List<Object[]> result = userRepository.findUserByIdNative(id);
            return ResponseEntity.ok(mapToUserDto(result.get(0)));
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
     * Lock/unlock user account (uses native update to avoid broken role FK)
     */
    @Transactional
    @PostMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            // Check user exists using native query
            if (userRepository.findUserByIdNative(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String status = request.get("status"); // ACTIVE or LOCKED
            if (!"ACTIVE".equals(status) && !"LOCKED".equals(status)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status. Must be ACTIVE or LOCKED"));
            }

            jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", status, id);

            // Return updated user
            List<Object[]> result = userRepository.findUserByIdNative(id);
            if (result.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(mapToUserDto(result.get(0)));
        } catch (Exception e) {
            log.error("Error updating user status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Delete user (uses native query to avoid broken role FK)
     */
    @Transactional
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            // Check user exists using native count
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Long.class, id);
            if (count == null || count == 0) {
                return ResponseEntity.notFound().build();
            }

            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get all courses for admin moderation with pagination
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getAllCoursesForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<com._6.CourseManagerment.entity.Course> courses;

            if (search != null && !search.trim().isEmpty()) {
                courses = courseRepository.findByTitleContainingIgnoreCase(search.trim(), pageable);
            } else if (status != null && !status.trim().isEmpty()) {
                courses = courseRepository.findByStatus(status.trim(), pageable);
            } else {
                courses = courseRepository.findAll(pageable);
            }

            Page<Map<String, Object>> result = courses.map(course -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", course.getId());
                map.put("title", course.getTitle());
                map.put("status", course.getStatus());
                map.put("level", course.getLevel());
                map.put("price", course.getPrice());
                map.put("studentCount", course.getStudentCount());
                map.put("createdAt", course.getCreatedAt());
                map.put("instructorName", course.getInstructor() != null ? course.getInstructor().getFullName() : "Unknown");
                map.put("categoryName", course.getCategory() != null ? course.getCategory().getName() : "Unknown");
                return map;
            });

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching courses for admin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update course status (approve/reject/archive)
     */
    @Transactional
    @PutMapping("/courses/{id}/status")
    public ResponseEntity<?> updateCourseStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || (!status.equals("PUBLISHED") && !status.equals("REJECTED") && !status.equals("DRAFT") && !status.equals("ARCHIVED"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status. Must be PUBLISHED, REJECTED, DRAFT, or ARCHIVED"));
            }

            com._6.CourseManagerment.entity.Course course = courseRepository.findById(id).orElse(null);
            if (course == null) {
                return ResponseEntity.notFound().build();
            }

            course.setStatus(status);
            courseRepository.save(course);

            Map<String, Object> result = new HashMap<>();
            result.put("id", course.getId());
            result.put("title", course.getTitle());
            result.put("status", course.getStatus());
            result.put("message", "Course status updated to " + status);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating course status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get comprehensive platform statistics
     */
    @GetMapping("/statistics/overview")
    public ResponseEntity<?> getOverviewStatistics() {
        try {
            long totalUsers = userRepository.count();
            long totalCourses = courseRepository.count();
            long totalEnrollments = enrollmentRepository.count();

            Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
            Role instructorRole = roleRepository.findByName("INSTRUCTOR").orElse(null);
            Role studentRole = roleRepository.findByName("STUDENT").orElse(null);

            long adminCount = adminRole != null ? userRepository.countByRoleIdNative(adminRole.getId()) : 0;
            long instructorCount = instructorRole != null ? userRepository.countByRoleIdNative(instructorRole.getId()) : 0;
            long studentCount = studentRole != null ? userRepository.countByRoleIdNative(studentRole.getId()) : 0;

            long publishedCourses = courseRepository.countByStatus("PUBLISHED");
            long draftCourses = courseRepository.countByStatus("DRAFT");
            long archivedCourses = courseRepository.countByStatus("ARCHIVED");

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("totalCourses", totalCourses);
            stats.put("totalEnrollments", totalEnrollments);
            stats.put("adminCount", adminCount);
            stats.put("instructorCount", instructorCount);
            stats.put("studentCount", studentCount);
            stats.put("publishedCourses", publishedCourses);
            stats.put("draftCourses", draftCourses);
            stats.put("archivedCourses", archivedCourses);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching overview statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get top courses by student count
     */
    @GetMapping("/statistics/top-courses")
    public ResponseEntity<?> getTopCourses() {
        try {
            Pageable top5 = PageRequest.of(0, 5);
            List<com._6.CourseManagerment.entity.Course> courses = courseRepository.findFeaturedCourses(top5);
            List<Map<String, Object>> result = courses.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("title", c.getTitle());
                m.put("studentCount", c.getStudentCount() != null ? c.getStudentCount() : 0);
                m.put("rating", c.getRating() != null ? c.getRating() : 0);
                m.put("instructorName", c.getInstructor() != null ? c.getInstructor().getFullName() : "Unknown");
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching top courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all categories for admin management
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<Category> categories = categoryRepository.findAll();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new category
     */
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Category name is required"));
            }

            if (categoryRepository.existsByName(name.trim())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Category already exists: " + name));
            }

            Category category = new Category();
            category.setName(name.trim());
            category.setDescription(description != null ? description.trim() : null);

            Category saved = categoryRepository.save(category);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error creating category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update a category
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            if (category == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.containsKey("name")) {
                String name = request.get("name").trim();
                if (!name.isEmpty() && !name.equals(category.getName()) && categoryRepository.existsByName(name)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Category already exists: " + name));
                }
                category.setName(name);
            }
            if (request.containsKey("description")) {
                category.setDescription(request.get("description"));
            }

            Category saved = categoryRepository.save(category);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error updating category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a category
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            if (!categoryRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            categoryRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting category", e);
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
     * Helper method to convert User to UserDto (handles broken role FK gracefully)
     */
    @SuppressWarnings("unused")
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        try {
            dto.setRole(user.getRole() != null ? user.getRole().getName() : null);
        } catch (Exception e) {
            // Role FK is broken (role_id points to non-existent role)
            dto.setRole(null);
        }
        return dto;
    }
}
