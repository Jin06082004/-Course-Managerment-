package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.EnrollmentDto;
import com._6.CourseManagerment.dto.PageResponse;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.EnrollmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/enrollments")
@Slf4j
public class EnrollmentController {
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    /**
     * Get my enrollments (requires authentication)
     */
    @GetMapping("/my-courses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrollmentDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        try {
            Long userId = extractUserIdFromAuth();
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<EnrollmentDto> enrollments = enrollmentService.getMyEnrollments(userId, pageable);
            return ResponseEntity.ok(toPageResponse(enrollments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Get active enrollments
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getActiveEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = extractUserIdFromAuth();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastAccessedDate"));
            Page<EnrollmentDto> enrollments = enrollmentService.getActiveEnrollments(userId, pageable);
            return ResponseEntity.ok(toPageResponse(enrollments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Get completed enrollments
     */
    @GetMapping("/completed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCompletedEnrollments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = extractUserIdFromAuth();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completionDate"));
            Page<EnrollmentDto> enrollments = enrollmentService.getCompletedEnrollments(userId, pageable);
            return ResponseEntity.ok(toPageResponse(enrollments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Enroll in a course
     */
    @PostMapping("/enroll/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enrollInCourse(@PathVariable Long courseId) {
        try {
            Long userId = extractUserIdFromAuth();
            EnrollmentDto enrollment = enrollmentService.enrollUserInCourse(userId, courseId);
            return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Check if user is enrolled in course
     */
    @GetMapping("/check/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> isEnrolled(@PathVariable Long courseId) {
        try {
            Long userId = extractUserIdFromAuth();
            var status = enrollmentService.getEnrollmentStatus(userId, courseId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Get enrollment by ID
     */
    @GetMapping("/{enrollmentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getEnrollmentById(@PathVariable Long enrollmentId) {
        try {
            EnrollmentDto enrollment = enrollmentService.getEnrollmentById(enrollmentId);
            return ResponseEntity.ok(enrollment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Update progress
     */
    @PutMapping("/{enrollmentId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProgress(
            @PathVariable Long enrollmentId,
            @RequestParam Float progressPercentage) {
        try {
            EnrollmentDto enrollment = enrollmentService.updateProgress(enrollmentId, progressPercentage);
            return ResponseEntity.ok(enrollment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Complete course
     */
    @PutMapping("/{enrollmentId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> completeCourse(@PathVariable Long enrollmentId) {
        try {
            EnrollmentDto enrollment = enrollmentService.completeEnrollment(enrollmentId);
            return ResponseEntity.ok(enrollment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Unenroll from course
     */
    @DeleteMapping("/{courseId}/unenroll")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unenrollFromCourse(@PathVariable Long courseId) {
        try {
            Long userId = extractUserIdFromAuth();
            enrollmentService.unenrollUserFromCourse(userId, courseId);
            return ResponseEntity.ok(Collections.singletonMap("message", "Successfully unenrolled from course"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Get course enrollments (Admin/Instructor)
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> getCourseEnrollments(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<EnrollmentDto> enrollments = enrollmentService.getCourseEnrollments(courseId, pageable);
            return ResponseEntity.ok(toPageResponse(enrollments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
    
    /**
     * Extract user ID from JWT token stored in SecurityContext
     */
    private Long extractUserIdFromAuth() throws Exception {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new Exception("User not authenticated or userId not found in token");
        }
        return userId;
    }
    
    /**
     * Convert Spring Page to flat PageResponse — avoids PageImpl Hibernate proxy serialization issues.
     */
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
