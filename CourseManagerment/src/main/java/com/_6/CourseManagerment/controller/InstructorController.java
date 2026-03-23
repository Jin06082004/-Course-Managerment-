package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.CourseDto;
import com._6.CourseManagerment.dto.CreateCourseRequest;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    /**
     * Get all courses created by the current instructor
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getInstructorCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        try {
            User instructor = SecurityUtils.getCurrentUser(auth);
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
            User instructor = SecurityUtils.getCurrentUser(auth);
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
            User instructor = SecurityUtils.getCurrentUser(auth);
            if (instructor == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            CourseDto createdCourse = courseService.createCourse(request, instructor.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
        } catch (Exception e) {
            log.error("Error creating course", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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
            User instructor = SecurityUtils.getCurrentUser(auth);
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
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(
            @PathVariable Long id,
            Authentication auth) {
        try {
            User instructor = SecurityUtils.getCurrentUser(auth);
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
     * Get course statistics for instructor
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getInstructorStatistics(Authentication auth) {
        try {
            User instructor = SecurityUtils.getCurrentUser(auth);
            
            List<Course> courses = courseRepository.findByInstructor(instructor);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCourses", courses.size());
            stats.put("activeCourses", courses.stream()
                    .filter(c -> "ACTIVE".equals(c.getStatus()))
                    .count());
            stats.put("draftCourses", courses.stream()
                    .filter(c -> "DRAFT".equals(c.getStatus()))
                    .count());
            stats.put("totalStudents", courses.stream()
                    .mapToLong(c -> Long.valueOf(c.getStudentCount() != null ? c.getStudentCount() : 0))
                    .sum());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching statistics", e);
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
        if (course.getInstructor() != null) {
            dto.setInstructorId(course.getInstructor().getId());
            dto.setInstructorName(course.getInstructor().getFullName());
        }
        dto.setStudentCount((course.getStudentCount() != null ? course.getStudentCount() : 0));
        return dto;
    }
}
