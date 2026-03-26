package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.AIChatRequest;
import com._6.CourseManagerment.dto.AIChatResponse;
import com._6.CourseManagerment.dto.ChatHistoryResponse;
import com._6.CourseManagerment.dto.ChatMessageDTO;
import com._6.CourseManagerment.dto.LessonSummaryDTO;
import com._6.CourseManagerment.dto.SummaryResponse;
import com._6.CourseManagerment.entity.ChatMessage;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.ChatMessageRepository;
import com._6.CourseManagerment.repository.UserRepository;
import com._6.CourseManagerment.security.JwtTokenProvider;
import com._6.CourseManagerment.service.AIAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AIAssistantController {

    private final AIAssistantService aiAssistantService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Send message to AI and get response
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> chat(
            @RequestBody AIChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            // Get current user from token
            User user = getCurrentUser(token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AIChatResponse.builder()
                                .success(false)
                                .error("User not authenticated")
                                .build());
            }

            // Validate request
            if (request.getLessonId() == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AIChatResponse.builder()
                                .success(false)
                                .error("lessonId and message are required")
                                .build());
            }

            // Process the chat
            AIChatResponse response = aiAssistantService.chat(request, user);
            
            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            log.error("Error in chat endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AIChatResponse.builder()
                            .success(false)
                            .error("Server error: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get chat history for a specific lesson
     * GET /api/ai/history/{lessonId}
     */
    @GetMapping("/history/{lessonId}")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable Long lessonId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            User user = getCurrentUser(token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ChatHistoryResponse.builder()
                                .success(false)
                                .build());
            }

            List<ChatMessage> messages = chatMessageRepository.findChatHistoryByLessonAndUser(lessonId, user.getId());
            
            List<ChatMessageDTO> dtos = messages.stream()
                    .map(msg -> ChatMessageDTO.builder()
                            .id(msg.getId())
                            .content(msg.getContent())
                            .aiResponse(msg.getAiResponse())
                            .senderType(msg.getSenderType())
                            .createdAt(msg.getCreatedAt())
                            .userId(msg.getUser().getId())
                            .lessonId(msg.getLesson().getId())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ChatHistoryResponse.builder()
                    .messages(dtos)
                    .totalMessages(dtos.size())
                    .success(true)
                    .build());

        } catch (Exception e) {
            log.error("Error getting chat history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ChatHistoryResponse.builder()
                            .success(false)
                            .build());
        }
    }

    /**
     * Clear chat history for a lesson
     * DELETE /api/ai/history/{lessonId}
     */
    @DeleteMapping("/history/{lessonId}")
    public ResponseEntity<AIChatResponse> clearChatHistory(
            @PathVariable Long lessonId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            User user = getCurrentUser(token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AIChatResponse.builder()
                                .success(false)
                                .error("User not authenticated")
                                .build());
            }

            List<ChatMessage> messages = chatMessageRepository.findChatHistoryByLessonAndUser(lessonId, user.getId());
            chatMessageRepository.deleteAll(messages);

            return ResponseEntity.ok(AIChatResponse.builder()
                    .success(true)
                    .build());

        } catch (Exception e) {
            log.error("Error clearing chat history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AIChatResponse.builder()
                            .success(false)
                            .error("Error clearing history")
                            .build());
        }
    }

    /**
     * Get suggested questions for a lesson
     * GET /api/ai/suggestions/{lessonId}
     */
    @GetMapping("/suggestions/{lessonId}")
    public ResponseEntity<List<String>> getSuggestedQuestions(@PathVariable Long lessonId) {
        try {
            List<String> suggestions = aiAssistantService.getSuggestedQuestions(lessonId);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error getting suggestions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate or get summary for a lesson
     * GET /api/ai/summary/{lessonId}
     */
    @GetMapping("/summary/{lessonId}")
    public ResponseEntity<SummaryResponse> getSummary(@PathVariable Long lessonId) {
        try {
            long startTime = System.currentTimeMillis();
            
            LessonSummaryDTO summary = aiAssistantService.generateSummary(lessonId);
            
            long generationTime = System.currentTimeMillis() - startTime;
            
            String message = summary.getIsFromCache() 
                ? "Summary loaded from cache" 
                : "Summary generated successfully";
            
            return ResponseEntity.ok(SummaryResponse.builder()
                    .success(true)
                    .message(message)
                    .summary(summary)
                    .generationTimeMs(generationTime)
                    .build());

        } catch (Exception e) {
            log.error("Error getting summary for lesson {}", lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SummaryResponse.builder()
                            .success(false)
                            .error("Error generating summary: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Extract current user from JWT token
     */
    private User getCurrentUser(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                if (jwtTokenProvider.validateToken(jwt)) {
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    Optional<User> userOpt = userRepository.findByUsername(username);
                    return userOpt.orElse(null);
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting user from token", e);
            return null;
        }
    }
}
