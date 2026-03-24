package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.CourseDto;
import com._6.CourseManagerment.dto.CreateCourseRequest;
import com._6.CourseManagerment.dto.PageResponse;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    /**
     * Get all courses with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<CourseDto> courses = courseService.getAllCourses(pageable);
            return ResponseEntity.ok(toPageResponse(courses));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Get course by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            CourseDto course = courseService.getCourseById(id);
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Search courses
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchCourses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CourseDto> courses = courseService.searchCourses(title, categoryId, level, pageable);
            return ResponseEntity.ok(toPageResponse(courses));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Get featured courses
     */
    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            List<CourseDto> courses = courseService.getFeaturedCourses(pageable);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Get courses by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getCoursesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CourseDto> courses = courseService.getCoursesByCategory(categoryId, pageable);
            return ResponseEntity.ok(toPageResponse(courses));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Get courses by instructor
     */
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<?> getCoursesByInstructor(
            @PathVariable Long instructorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CourseDto> courses = courseService.getCoursesByInstructor(instructorId, pageable);
            return ResponseEntity.ok(toPageResponse(courses));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Create a new course (Instructor/Admin only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication) {
        try {
            // Get instructor ID from authentication
            Long instructorId = extractUserIdFromAuth(authentication);
            CourseDto createdCourse = courseService.createCourse(request, instructorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Update course (Instructor/Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication) {
        try {
            CourseDto updatedCourse = courseService.updateCourse(id, request);
            return ResponseEntity.ok(updatedCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Delete course (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("message", "Course deleted successfully");
            }});
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Publish course (Instructor/Admin only)
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> publishCourse(@PathVariable Long id) {
        try {
            CourseDto publishedCourse = courseService.publishCourse(id);
            return ResponseEntity.ok(publishedCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Archive course (Admin only)
     */
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> archiveCourse(@PathVariable Long id) {
        try {
            CourseDto archivedCourse = courseService.archiveCourse(id);
            return ResponseEntity.ok(archivedCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }
    
    /**
     * Helper method to extract user ID from authentication
     */
    private Long extractUserIdFromAuth(Authentication authentication) {
        Long userId = SecurityUtils.getCurrentUserId();
        return userId != null ? userId : 1L;
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.isFirst(),
            page.isLast()
        );
    }
}
