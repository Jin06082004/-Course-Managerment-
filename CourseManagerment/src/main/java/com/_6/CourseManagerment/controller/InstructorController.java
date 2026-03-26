package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.CourseDto;
import com._6.CourseManagerment.dto.CreateCourseRequest;
import com._6.CourseManagerment.dto.CreateResourceRequest;
import com._6.CourseManagerment.dto.ResourceDto;
import com._6.CourseManagerment.entity.Category;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.Enrollment;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.CategoryRepository;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import com._6.CourseManagerment.repository.UserRepository;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.CourseService;
import com._6.CourseManagerment.service.ResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Instructor Controller - Handles instructor-specific operations
 * Only users with INSTRUCTOR role can access these endpoints
 */
@RestController
@RequestMapping("/api/instructor")
@PreAuthorize("hasRole('INSTRUCTOR')")
@Slf4j
public class InstructorController {
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceService resourceService;

    private User getCurrentInstructor(Authentication auth) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    /**
     * Get all categories (for dropdown selection)
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
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
     * Get all courses created by the current instructor
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getInstructorCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            if (instructor == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Course> courses = courseRepository.findByInstructor(instructor, pageable);
            Page<CourseDto> courseDtos = courses.map(this::convertCourseToDto);
            
            return ResponseEntity.ok(courseDtos);
        } catch (Exception e) {
            log.error("Error fetching instructor courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get course details (only if instructor owns the course)
     */
    @GetMapping("/courses/{id}")
    public ResponseEntity<?> getInstructorCourse(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            Course course = courseRepository.findById(id).orElse(null);
            
            if (course == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if instructor owns this course
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not have permission to access this course"));
            }
            
            return ResponseEntity.ok(convertCourseToDto(course));
        } catch (Exception e) {
            log.error("Error fetching course", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Create a new course
     */
    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            if (instructor == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            CourseDto createdCourse = courseService.createCourse(request, instructor.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
        } catch (Exception e) {
            log.error("Error creating course", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add a resource link to an owned course
     */
    @PostMapping("/resources")
    public ResponseEntity<?> addResource(
            @Valid @RequestBody CreateResourceRequest request,
            Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            if (instructor == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }

            ResourceDto created = resourceService.addResource(request, instructor.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error adding resource", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update course (only if instructor owns it)
     */
    @PutMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            Course course = courseRepository.findById(id).orElse(null);
            
            if (course == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if instructor owns this course
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not have permission to update this course"));
            }
            
            // Update course fields
            if (updates.containsKey("title")) {
                course.setTitle((String) updates.get("title"));
            }
            if (updates.containsKey("description")) {
                course.setDescription((String) updates.get("description"));
            }
            if (updates.containsKey("level")) {
                course.setLevel((String) updates.get("level"));
            }
            if (updates.containsKey("status")) {
                course.setStatus((String) updates.get("status"));
            }
            if (updates.containsKey("price")) {
                Object priceVal = updates.get("price");
                if (priceVal instanceof Number) {
                    course.setPrice(BigDecimal.valueOf(((Number) priceVal).doubleValue()));
                } else if (priceVal instanceof String) {
                    course.setPrice(new BigDecimal((String) priceVal));
                }
            }
            if (updates.containsKey("duration")) {
                Object durVal = updates.get("duration");
                if (durVal instanceof Number) {
                    course.setDuration(((Number) durVal).intValue());
                } else if (durVal instanceof String) {
                    course.setDuration(Integer.parseInt((String) durVal));
                }
            }
            if (updates.containsKey("thumbnailUrl")) {
                course.setThumbnailUrl((String) updates.get("thumbnailUrl"));
            }
            if (updates.containsKey("videoUrl")) {
                course.setVideoUrl((String) updates.get("videoUrl"));
            }
            
            courseRepository.save(course);
            return ResponseEntity.ok(convertCourseToDto(course));
        } catch (Exception e) {
            log.error("Error updating course", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Delete course (only if instructor owns it)
     */
    @Transactional
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            Course course = courseRepository.findById(id).orElse(null);
            
            if (course == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if instructor owns this course
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not have permission to delete this course"));
            }
            
            courseRepository.delete(course);
            return ResponseEntity.ok(Map.of("message", "Course deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting course", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get enrolled students for a course (only if instructor owns the course)
     */
    @GetMapping("/courses/{id}/students")
    public ResponseEntity<?> getCourseStudents(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            Course course = courseRepository.findById(id).orElse(null);
            
            if (course == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not have permission to access this course"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Enrollment> enrollments = enrollmentRepository.findByCourse_Id(id, pageable);
            
            List<Map<String, Object>> students = enrollments.getContent().stream()
                    .map(enrollment -> {
                        Map<String, Object> student = new HashMap<>();
                        User user = enrollment.getUser();
                        student.put("id", user.getId());
                        student.put("username", user.getUsername());
                        student.put("fullName", user.getFullName());
                        student.put("email", user.getEmail());
                        student.put("avatar", user.getAvatar());
                        student.put("enrolledAt", enrollment.getEnrollmentDate());
                        student.put("status", enrollment.getStatus());
                        student.put("progress", enrollment.getProgressPercentage() != null ? Math.round(enrollment.getProgressPercentage()) : 0);
                        return student;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("students", students);
            result.put("totalElements", enrollments.getTotalElements());
            result.put("totalPages", enrollments.getTotalPages());
            result.put("number", enrollments.getNumber());
            result.put("size", enrollments.getSize());
            result.put("courseId", id);
            result.put("courseTitle", course.getTitle());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching course students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get course statistics for instructor
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getInstructorStatistics(Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            
            List<Course> courses = courseRepository.findByInstructor(instructor);
            
            long publishedCourses = courses.stream()
                    .filter(c -> "PUBLISHED".equals(c.getStatus()))
                    .count();
            long archivedCourses = courses.stream()
                    .filter(c -> "ARCHIVED".equals(c.getStatus()))
                    .count();
            long draftCourses = courses.stream()
                    .filter(c -> "DRAFT".equals(c.getStatus()))
                    .count();
            
            long totalStudents = courses.stream()
                    .mapToLong(c -> Long.valueOf(c.getStudentCount() != null ? c.getStudentCount() : 0))
                    .sum();
            
            // Revenue calculation (price * studentCount per published course)
            BigDecimal totalRevenue = courses.stream()
                    .filter(c -> "PUBLISHED".equals(c.getStatus()))
                    .map(c -> {
                        BigDecimal price = c.getPrice() != null ? c.getPrice() : BigDecimal.ZERO;
                        int count = c.getStudentCount() != null ? c.getStudentCount() : 0;
                        return price.multiply(BigDecimal.valueOf(count));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Per-course performance data
            List<Map<String, Object>> coursePerformance = courses.stream()
                    .map(c -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", c.getId());
                        m.put("title", c.getTitle());
                        m.put("status", c.getStatus());
                        m.put("studentCount", c.getStudentCount() != null ? c.getStudentCount() : 0);
                        m.put("rating", c.getRating() != null ? c.getRating() : 0.0f);
                        m.put("level", c.getLevel());
                        BigDecimal price = c.getPrice() != null ? c.getPrice() : BigDecimal.ZERO;
                        int sCount = c.getStudentCount() != null ? c.getStudentCount() : 0;
                        m.put("revenue", price.multiply(BigDecimal.valueOf(sCount)));
                        return m;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCourses", courses.size());
            stats.put("publishedCourses", publishedCourses);
            stats.put("draftCourses", draftCourses);
            stats.put("archivedCourses", archivedCourses);
            stats.put("totalStudents", totalStudents);
            stats.put("totalRevenue", totalRevenue);
            stats.put("coursePerformance", coursePerformance);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get earnings data for instructor
     */
    @GetMapping("/earnings")
    public ResponseEntity<?> getEarnings(Authentication auth) {
        try {
            User instructor = getCurrentInstructor(auth);
            
            List<Course> courses = courseRepository.findByInstructor(instructor);
            
            List<Map<String, Object>> courseEarnings = courses.stream()
                    .filter(c -> "PUBLISHED".equals(c.getStatus()))
                    .map(course -> {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("courseId", course.getId());
                        entry.put("courseTitle", course.getTitle());
                        entry.put("price", course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO);
                        int studentCount = course.getStudentCount() != null ? course.getStudentCount() : 0;
                        entry.put("studentCount", studentCount);
                        BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
                        entry.put("revenue", price.multiply(BigDecimal.valueOf(studentCount)));
                        return entry;
                    })
                    .collect(Collectors.toList());
            
            BigDecimal totalRevenue = courseEarnings.stream()
                    .map(e -> (BigDecimal) e.get("revenue"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> earnings = new HashMap<>();
            earnings.put("courseEarnings", courseEarnings);
            earnings.put("totalRevenue", totalRevenue);
            earnings.put("publishedCourseCount", courseEarnings.size());
            
            return ResponseEntity.ok(earnings);
        } catch (Exception e) {
            log.error("Error fetching earnings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Helper method to convert Course to CourseDto
     */
    private CourseDto convertCourseToDto(Course course) {
        CourseDto dto = new CourseDto();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setLevel(course.getLevel());
        dto.setPrice(course.getPrice());
        dto.setStatus(course.getStatus());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setDuration(course.getDuration());
        dto.setThumbnailUrl(course.getThumbnailUrl());
        dto.setVideoUrl(course.getVideoUrl());
        dto.setCode(course.getCode());
        if (course.getCategory() != null) {
            dto.setCategoryId(course.getCategory().getId());
            dto.setCategoryName(course.getCategory().getName());
        }
        if (course.getInstructor() != null) {
            dto.setInstructorId(course.getInstructor().getId());
            dto.setInstructorName(course.getInstructor().getFullName());
        }
        dto.setStudentCount((course.getStudentCount() != null ? course.getStudentCount() : 0));
        return dto;
    }
}
