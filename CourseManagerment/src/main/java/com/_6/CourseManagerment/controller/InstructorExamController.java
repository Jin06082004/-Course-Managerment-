package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.*;
import com._6.CourseManagerment.service.ExamService;
import com._6.CourseManagerment.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for instructor exam management
 */
@RestController
@RequestMapping("/api/instructor/exams")
@RequiredArgsConstructor
@Slf4j
public class InstructorExamController {

    private final ExamService examService;
    private final SecurityUtil securityUtil;

    /**
     * Create a new exam
     * POST /api/instructor/exams?courseId=1
     */
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> createExam(
            @RequestParam Long courseId,
            @RequestBody CreateExamRequest request) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            ExamDTO exam = examService.createExam(courseId, instructorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ExamResponse.builder()
                            .success(true)
                            .message("Exam created successfully")
                            .exam(exam)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for exam creation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error creating exam", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Failed to create exam: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Update existing exam
     * PUT /api/instructor/exams/{examId}
     */
    @PutMapping("/{examId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> updateExam(
            @PathVariable Long examId,
            @RequestBody CreateExamRequest request) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            ExamDTO exam = examService.updateExam(examId, instructorId, request);
            return ResponseEntity.ok(
                    ExamResponse.builder()
                            .success(true)
                            .message("Exam updated successfully")
                            .exam(exam)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized exam update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Not authorized to update this exam")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for exam update: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error updating exam", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Failed to update exam: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Delete exam (draft only)
     * DELETE /api/instructor/exams/{examId}
     */
    @DeleteMapping("/{examId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> deleteExam(@PathVariable Long examId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            examService.deleteExam(examId, instructorId);
            return ResponseEntity.ok(
                    ExamResponse.builder()
                            .success(true)
                            .message("Exam deleted successfully")
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized exam deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Not authorized to delete this exam")
                            .build()
            );
        } catch (IllegalStateException e) {
            log.warn("Cannot delete published exam: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error deleting exam", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Failed to delete exam: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get single exam
     * GET /api/instructor/exams/{examId}
     */
    @GetMapping("/{examId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> getExam(@PathVariable Long examId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            ExamDTO exam = examService.getExamById(examId, instructorId);
            return ResponseEntity.ok(
                    ExamResponse.builder()
                            .success(true)
                            .exam(exam)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized exam view: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Not authorized to view this exam")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Exam not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching exam", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Failed to fetch exam: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get all exams for instructor
     * GET /api/instructor/exams
     */
    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> getMyExams(
            @RequestParam(required = false) Long courseId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            List<ExamDTO> exams;

            if (courseId != null) {
                exams = examService.getExamsByCourseAndInstructor(courseId, instructorId);
            } else {
                exams = examService.getExamsByInstructor(instructorId);
            }

            return ResponseEntity.ok(
                    ExamListResponse.builder()
                            .success(true)
                            .exams(exams)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching exams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamListResponse.builder()
                            .success(false)
                            .error("Failed to fetch exams: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Publish exam
     * PUT /api/instructor/exams/{examId}/publish
     */
    @PutMapping("/{examId}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> publishExam(@PathVariable Long examId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            ExamDTO exam = examService.publishExam(examId, instructorId);
            return ResponseEntity.ok(
                    ExamResponse.builder()
                            .success(true)
                            .message("Exam published successfully")
                            .exam(exam)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized exam publication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Not authorized to publish this exam")
                            .build()
            );
        } catch (IllegalStateException e) {
            log.warn("Cannot publish invalid exam: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error publishing exam", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Failed to publish exam: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get exam results
     * GET /api/instructor/exams/{examId}/results
     */
    @GetMapping("/{examId}/results")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> getExamResults(@PathVariable Long examId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            List<ExamResultDTO> results = examService.getExamResults(examId, instructorId);
            return ResponseEntity.ok(
                    ExamListResponse.builder()
                            .success(true)
                            .results(results)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized result access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ExamListResponse.builder()
                            .success(false)
                            .error("Not authorized to view these results")
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching exam results", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamListResponse.builder()
                            .success(false)
                            .error("Failed to fetch exam results: " + e.getMessage())
                            .build()
            );
        }
    }
}
