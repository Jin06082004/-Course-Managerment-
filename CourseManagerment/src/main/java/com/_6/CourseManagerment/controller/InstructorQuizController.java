package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.CreateQuizRequest;
import com._6.CourseManagerment.dto.QuizDTO;
import com._6.CourseManagerment.dto.QuizQuestionDto;
import com._6.CourseManagerment.dto.QuizResponse;
import com._6.CourseManagerment.dto.QuizListResponse;
import com._6.CourseManagerment.service.QuizService;
import com._6.CourseManagerment.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for instructor quiz management (quiz builder)
 * All endpoints require INSTRUCTOR_ROLE authorization
 */
@RestController
@RequestMapping("/api/instructor/quizzes")
@RequiredArgsConstructor
@Slf4j
public class InstructorQuizController {

    private final QuizService quizService;
    private final SecurityUtil securityUtil;

    /**
     * Create a new quiz
     * POST /api/instructor/quizzes?courseId=1
     */
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> createQuiz(
            @RequestParam Long courseId,
            @RequestBody CreateQuizRequest request) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            QuizDTO quiz = quizService.createQuiz(courseId, instructorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    QuizResponse.builder()
                            .success(true)
                            .message("Quiz created successfully")
                            .quiz(quiz)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for quiz creation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    QuizResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error creating quiz", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to create quiz: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Update existing quiz
     * PUT /api/instructor/quizzes/{quizId}
     */
    @PutMapping("/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> updateQuiz(
            @PathVariable Long quizId,
            @RequestBody CreateQuizRequest request) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            QuizDTO quiz = quizService.updateQuiz(quizId, instructorId, request);
            return ResponseEntity.ok(
                    QuizResponse.builder()
                            .success(true)
                            .message("Quiz updated successfully")
                            .quiz(quiz)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized quiz update attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Not authorized to update this quiz")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for quiz update: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    QuizResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error updating quiz", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to update quiz: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Delete quiz (only draft quizzes)
     * DELETE /api/instructor/quizzes/{quizId}
     */
    @DeleteMapping("/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long quizId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            quizService.deleteQuiz(quizId, instructorId);
            return ResponseEntity.ok(
                    QuizResponse.builder()
                            .success(true)
                            .message("Quiz deleted successfully")
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized quiz deletion attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Not authorized to delete this quiz")
                            .build()
            );
        } catch (IllegalStateException e) {
            log.warn("Cannot delete published quiz: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    QuizResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error deleting quiz", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to delete quiz: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get single quiz by ID
     * GET /api/instructor/quizzes/{quizId}
     */
    @GetMapping("/{quizId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> getQuiz(@PathVariable Long quizId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            QuizDTO quiz = quizService.getQuizById(quizId, instructorId);
            return ResponseEntity.ok(
                    QuizResponse.builder()
                            .success(true)
                            .quiz(quiz)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized quiz view attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Not authorized to view this quiz")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Quiz not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    QuizResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching quiz", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to fetch quiz: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get all quizzes for instructor
     * GET /api/instructor/quizzes
     * Optional query params: courseId={courseId}
     */
    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> getMyQuizzes(
            @RequestParam(required = false) Long courseId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            List<QuizDTO> quizzes;

            if (courseId != null) {
                quizzes = quizService.getQuizzesByCourseAndInstructor(courseId, instructorId);
            } else {
                quizzes = quizService.getQuizzesByInstructor(instructorId);
            }

            return ResponseEntity.ok(
                    QuizListResponse.builder()
                            .success(true)
                            .quizzes(quizzes)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching quizzes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizListResponse.builder()
                            .success(false)
                            .error("Failed to fetch quizzes: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get draft quizzes for instructor
     * GET /api/instructor/quizzes/drafts
     */
    @GetMapping("/drafts")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> getDraftQuizzes() {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            List<QuizDTO> quizzes = quizService.getDraftsByInstructor(instructorId);
            return ResponseEntity.ok(
                    QuizListResponse.builder()
                            .success(true)
                            .quizzes(quizzes)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error fetching draft quizzes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizListResponse.builder()
                            .success(false)
                            .error("Failed to fetch draft quizzes: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Add question to quiz
     * POST /api/instructor/quizzes/{quizId}/questions
     */
    @PostMapping("/{quizId}/questions")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> addQuestion(
            @PathVariable Long quizId,
            @RequestBody QuizQuestionDto questionDTO) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            QuizDTO quiz = quizService.addQuestion(quizId, instructorId, questionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    QuizResponse.builder()
                            .success(true)
                            .message("Question added successfully")
                            .quiz(quiz)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized question addition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Not authorized to modify this quiz")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for question: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    QuizResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error adding question", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to add question: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Remove question from quiz
     * DELETE /api/instructor/quizzes/{quizId}/questions/{questionId}
     */
    @DeleteMapping("/{quizId}/questions/{questionId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> removeQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            QuizDTO quiz = quizService.removeQuestion(quizId, questionId, instructorId);
            return ResponseEntity.ok(
                    QuizResponse.builder()
                            .success(true)
                            .message("Question removed successfully")
                            .quiz(quiz)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized question removal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Not authorized to modify this quiz")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid input for question removal: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    QuizResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error removing question", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to remove question: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Publish quiz
     * PUT /api/instructor/quizzes/{quizId}/publish
     */
    @PutMapping("/{quizId}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> publishQuiz(@PathVariable Long quizId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            QuizDTO quiz = quizService.publishQuiz(quizId, instructorId);
            return ResponseEntity.ok(
                    QuizResponse.builder()
                            .success(true)
                            .message("Quiz published successfully")
                            .quiz(quiz)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized quiz publication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Not authorized to publish this quiz")
                            .build()
            );
        } catch (IllegalStateException e) {
            log.warn("Cannot publish invalid quiz: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    QuizResponse.builder()
                            .success(false)
                            .error(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error publishing quiz", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to publish quiz: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Save quiz as draft
     * PUT /api/instructor/quizzes/{quizId}/draft
     */
    @PutMapping("/{quizId}/draft")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> saveDraft(@PathVariable Long quizId) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            QuizDTO quiz = quizService.saveDraft(quizId, instructorId);
            return ResponseEntity.ok(
                    QuizResponse.builder()
                            .success(true)
                            .message("Quiz saved as draft")
                            .quiz(quiz)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized quiz modification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Not authorized to modify this quiz")
                            .build()
            );
        } catch (Exception e) {
            log.error("Error saving draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to save draft: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Reorder questions (for drag & drop)
     * PUT /api/instructor/quizzes/{quizId}/reorder
     * Body: { "questionIds": [1, 3, 2] }
     */
    @PutMapping("/{quizId}/reorder")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> reorderQuestions(
            @PathVariable Long quizId,
            @RequestBody ReorderRequest request) {
        try {
            Long instructorId = securityUtil.getCurrentUserId();
            QuizDTO quiz = quizService.reorderQuestions(quizId, instructorId, request.getQuestionIds());
            return ResponseEntity.ok(
                    QuizResponse.builder()
                            .success(true)
                            .message("Questions reordered successfully")
                            .quiz(quiz)
                            .build()
            );
        } catch (SecurityException e) {
            log.warn("Unauthorized question reordering: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Not authorized to modify this quiz")
                            .build()
            );
        } catch (Exception e) {
            log.error("Error reordering questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    QuizResponse.builder()
                            .success(false)
                            .error("Failed to reorder questions: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * DTO for reorder request
     */
    public static class ReorderRequest {
        private List<Long> questionIds;

        public List<Long> getQuestionIds() {
            return questionIds;
        }

        public void setQuestionIds(List<Long> questionIds) {
            this.questionIds = questionIds;
        }
    }
}
