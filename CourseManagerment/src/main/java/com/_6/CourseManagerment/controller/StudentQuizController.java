package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.LessonQuizDto;
import com._6.CourseManagerment.dto.QuizSubmitRequest;
import com._6.CourseManagerment.dto.QuizSubmitResultDto;
import com._6.CourseManagerment.service.QuizService;
import com._6.CourseManagerment.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for student quiz access.
 * GET  /api/lessons/{lessonId}/quiz  – fetch published quiz for a lesson
 * POST /api/quiz/submit              – submit answers and get result
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class StudentQuizController {

    private final QuizService quizService;

    /**
     * Fetch the published quiz attached to a lesson.
     * Returns 404 when no quiz exists, 403 when quiz is not yet published.
     */
    @GetMapping("/api/lessons/{lessonId}/quiz")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getQuizForLesson(@PathVariable Long lessonId) {
        try {
            LessonQuizDto quiz = quizService.getPublishedQuizForLesson(lessonId);
            return ResponseEntity.ok(quiz);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching quiz for lesson {}: {}", lessonId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load quiz"));
        }
    }

    /**
     * Submit quiz answers and return score + per-question result.
     */
    @PostMapping("/api/quiz/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitQuiz(@RequestBody QuizSubmitRequest request) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            QuizSubmitResultDto result = quizService.submitQuiz(request, userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error submitting quiz: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to submit quiz"));
        }
    }
}
