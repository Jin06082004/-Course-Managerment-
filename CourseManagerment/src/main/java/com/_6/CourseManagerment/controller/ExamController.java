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
 * REST API for exam management and submission
 */
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final ExamService examService;
    private final SecurityUtil securityUtil;

    /**
     * Get exam for student (published exams only)
     * GET /api/exams/{examId}
     */
    @GetMapping("/{examId}")
    public ResponseEntity<?> getExam(@PathVariable Long examId) {
        try {
            ExamDTO exam = examService.getExamForTaking(examId);
            return ResponseEntity.ok(
                    ExamResponse.builder()
                            .success(true)
                            .exam(exam)
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
     * Get published exams for a course
     * GET /api/exams/course/{courseId}
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getExamsByCourse(@PathVariable Long courseId) {
        try {
            List<ExamDTO> exams = examService.getPublishedExamsByCourse(courseId);
            return ResponseEntity.ok(
                    ExamListResponse.builder()
                            .success(true)
                            .exams(exams)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching exams by course", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamListResponse.builder()
                            .success(false)
                            .error("Failed to fetch exams: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Submit exam answers
     * POST /api/exams/submit
     */
    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitExam(@RequestBody SubmitExamRequest request) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            ExamResultDTO result = examService.submitExam(request.getExamId(), userId, request);
            return ResponseEntity.ok(
                    ExamResponse.builder()
                            .success(true)
                            .message("Exam submitted successfully")
                            .result(result)
                            .build()
            );
        } catch (IllegalStateException e) {
            log.warn("Exam submission failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid exam or user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error submitting exam", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Failed to submit exam: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get exam result for student
     * GET /api/exams/{examId}/result
     */
    @GetMapping("/{examId}/result")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getExamResult(@PathVariable Long examId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            ExamResultDTO result = examService.getExamResult(examId, userId);
            return ResponseEntity.ok(
                    ExamResponse.builder()
                            .success(true)
                            .result(result)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Exam result not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching exam result", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Failed to fetch exam result: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get student's exam results
     * GET /api/exams/my-results
     */
    @GetMapping("/my-results")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyExamResults() {
        try {
            Long userId = securityUtil.getCurrentUserId();
            List<ExamResultDTO> results = examService.getStudentExamResults(userId);
            return ResponseEntity.ok(
                    ExamListResponse.builder()
                            .success(true)
                            .results(results)
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

    /**
     * Get exam result by result ID
     * GET /api/exams/results/{resultId}
     */
    @GetMapping("/results/{resultId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getExamResultById(@PathVariable Long resultId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            ExamResultDTO result = examService.getExamResultById(resultId, userId);
            return ResponseEntity.ok(
                    ExamResponse.builder()
                            .success(true)
                            .result(result)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Exam result not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ExamResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized access to exam result: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Not authorized to view this result")
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching exam result", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ExamResponse.builder()
                            .success(false)
                            .error("Failed to fetch exam result: " + e.getMessage())
                            .build()
            );
        }
    }
}
