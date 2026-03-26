package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.AIGenerateQuizRequest;
import com._6.CourseManagerment.dto.AIGenerateQuizResponse;
import com._6.CourseManagerment.dto.AIRecommendationResponse;
import com._6.CourseManagerment.service.AIQuizService;
import com._6.CourseManagerment.service.AIRecommendationService;
import com._6.CourseManagerment.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for AI-powered features
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AIQuizService aiQuizService;
    private final AIRecommendationService aiRecommendationService;
    private final SecurityUtil securityUtil;

    /**
     * Generate quiz questions using OpenAI
     * POST /api/ai/generate-quiz
     *
     * Request:
     * {
     *   "lessonId": 1,
     *   "numberOfQuestions": 5,
     *   "quizTitle": "Chapter 3 Quiz",
     *   "quizDescription": "Test your knowledge"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Quiz questions generated successfully",
     *   "questionsCount": 5,
     *   "questions": [
     *     {
     *       "question": "What is...",
     *       "options": ["A", "B", "C", "D"],
     *       "correctAnswer": "A",
     *       "explanation": "Because..."
     *     }
     *   ]
     * }
     */
    @PostMapping("/generate-quiz")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> generateQuiz(@RequestBody AIGenerateQuizRequest request) {
        try {
            // Verify instructor is authenticated
            Long instructorId = securityUtil.getCurrentUserId();
            log.info("Instructor {} requesting AI quiz generation for lesson: {}", 
                    instructorId, request.getLessonId());

            // Generate quiz using AI
            AIGenerateQuizResponse response = aiQuizService.generateQuiz(request);

            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error generating AI quiz", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    AIGenerateQuizResponse.builder()
                            .success(false)
                            .error("Error generating quiz: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get AI-powered course recommendations for a user
     * GET /api/ai/recommend/{userId}
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Recommendations generated successfully",
     *   "recommendationCount": 5,
     *   "analysisReason": "Based on your learning history, progress, and interests",
     *   "recommendations": [
     *     {
     *       "id": 15,
     *       "title": "Advanced Java Programming",
     *       "description": "...",
     *       "category": "Programming",
     *       "level": "ADVANCED",
     *       "price": 29.99,
     *       "duration": 40,
     *       "rating": 4.8,
     *       "studentCount": 1250,
     *       "thumbnailUrl": "...",
     *       "reason": "Recommended because you've completed intermediate Java courses",
     *       "relevanceScore": 92.0
     *     }
     *   ]
     * }
     */
    @GetMapping("/recommend/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRecommendations(@PathVariable Long userId) {
        try {
            // Verify user is requesting their own recommendations or is admin
            Long currentUserId = securityUtil.getCurrentUserId();
            if (!userId.equals(currentUserId) && !securityUtil.isAdmin()) {
                log.warn("User {} attempted to access recommendations for user {}", currentUserId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        AIRecommendationResponse.builder()
                                .success(false)
                                .error("You can only access your own recommendations")
                                .build()
                );
            }

            log.info("Generating recommendations for user: {}", userId);

            // Get recommendations using AI
            AIRecommendationResponse response = aiRecommendationService.getRecommendations(userId);

            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error getting recommendations for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    AIRecommendationResponse.builder()
                            .success(false)
                            .error("Error generating recommendations: " + e.getMessage())
                            .build()
            );
        }
    }
}
