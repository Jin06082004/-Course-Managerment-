package com._6.CourseManagerment.service;

import com._6.CourseManagerment.config.OpenAIProperties;
import com._6.CourseManagerment.dto.AIChatRequest;
import com._6.CourseManagerment.dto.AIChatResponse;
import com._6.CourseManagerment.dto.LessonSummaryDTO;
import com._6.CourseManagerment.entity.ChatMessage;
import com._6.CourseManagerment.entity.LessonSummary;
import com._6.CourseManagerment.entity.Resource;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.ChatMessageRepository;
import com._6.CourseManagerment.repository.LessonSummaryRepository;
import com._6.CourseManagerment.repository.ResourceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIAssistantService {

    private final ChatMessageRepository chatMessageRepository;
    private final ResourceRepository resourceRepository;
    private final LessonSummaryRepository lessonSummaryRepository;
    private final OpenAIProperties openAIProperties;
    private final ObjectMapper objectMapper;

    /**
     * Process user message and get AI response from OpenAI
     */
    @Transactional
    public AIChatResponse chat(AIChatRequest request, User user) {
        try {
            // Validate lesson exists
            Optional<Resource> lesson = resourceRepository.findById(request.getLessonId());
            if (lesson.isEmpty()) {
                log.error("Lesson not found: {}", request.getLessonId());
                return AIChatResponse.builder()
                        .success(false)
                        .error("Lesson not found")
                        .build();
            }

            // Get previous chat history for context
            List<ChatMessage> chatHistory = chatMessageRepository.findChatHistoryByLessonAndUser(
                    request.getLessonId(), user.getId());

            // Build context from lesson and previous messages
            String context = buildContext(lesson.get(), chatHistory);

            // Call OpenAI API
            String aiResponse = callOpenAI(request.getMessage(), context);

            // Save user message
            ChatMessage userMessage = new ChatMessage();
            userMessage.setLesson(lesson.get());
            userMessage.setUser(user);
            userMessage.setContent(request.getMessage());
            userMessage.setRole(ChatMessage.MessageRole.USER);
            chatMessageRepository.save(userMessage);

            // Save AI response
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setLesson(lesson.get());
            aiMessage.setUser(user);
            aiMessage.setContent(request.getMessage());
            aiMessage.setAiResponse(aiResponse);
            aiMessage.setRole(ChatMessage.MessageRole.ASSISTANT);
            ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);

            return AIChatResponse.builder()
                    .id(savedAiMessage.getId())
                    .userMessage(request.getMessage())
                    .aiResponse(aiResponse)
                    .timestamp(LocalDateTime.now())
                    .senderType("ai")
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return AIChatResponse.builder()
                    .success(false)
                    .error("Error processing your request: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Build context from lesson content and chat history
     */
    private String buildContext(Resource lesson, List<ChatMessage> chatHistory) {
        StringBuilder context = new StringBuilder();
        
        // Add lesson context
        context.append("Lesson Title: ").append(lesson.getTitle()).append("\n");
        context.append("Lesson Description/URL: ").append(lesson.getUrl()).append("\n\n");
        
        // Add recent chat history for continuity
        if (!chatHistory.isEmpty()) {
            context.append("Previous conversation:\n");
            for (ChatMessage msg : chatHistory) {
                if (msg.getRole() == ChatMessage.MessageRole.USER) {
                    context.append("User: ").append(msg.getContent()).append("\n");
                } else {
                    context.append("Assistant: ").append(msg.getAiResponse()).append("\n");
                }
            }
        }
        
        return context.toString();
    }

    /**
     * Call OpenAI API
     */
    private String callOpenAI(String userMessage, String context) {
        try {
            // Build the prompt with context
            String prompt = "You are a helpful AI assistant for online course learning. " +
                    "Use the following lesson context to answer the student's question accurately and helpfully.\n\n" +
                    "LESSON CONTEXT:\n" + context + "\n\n" +
                    "Student Question: " + userMessage + "\n\n" +
                    "Please provide a clear, educational answer related to the lesson content.";

            // For now, return a placeholder for development
            // In production, integrate with actual OpenAI API
            String response = callOpenAIAPI(prompt, 1000, 0.7);
            
            return response;

        } catch (Exception e) {
            log.error("Error calling OpenAI", e);
            return "I'm unable to process your request at the moment. Please try again later.";
        }
    }

    /**
     * Make the actual API call to OpenAI
     * Using simple HTTP client approach for OpenAI API v1
     */
    private String callOpenAIAPI(String prompt, Integer maxTokens, Double temperature) {
        try {
            // OpenAI API call using HTTP
            String apiKey = openAIProperties.getApiKey();
            String model = openAIProperties.getModel() != null ? openAIProperties.getModel() : "gpt-3.5-turbo";
            
            if (maxTokens == null) {
                maxTokens = openAIProperties.getMaxTokens() != null ? openAIProperties.getMaxTokens() : 1000;
            }
            if (temperature == null) {
                temperature = openAIProperties.getTemperature() != null ? openAIProperties.getTemperature() : 0.7;
            }

            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("OpenAI API key not configured");
                return "AI assistant is not configured. Please set up your OpenAI API key.";
            }

            // Create JSON payload
            String payload = "{" +
                    "\"model\": \"" + model + "\"," +
                    "\"messages\": [{\"role\": \"user\", \"content\": " + escapeJson(prompt) + "}]," +
                    "\"max_tokens\": " + maxTokens + "," +
                    "\"temperature\": " + temperature +
                    "}";

            // Make HTTP request to OpenAI
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"),
                    payload
            );

            okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            okhttp3.Response httpResponse = client.newCall(httpRequest).execute();
            
            if (!httpResponse.isSuccessful()) {
                log.error("OpenAI API error: {}", httpResponse.code());
                return "Error: Unable to get response from AI. Status: " + httpResponse.code();
            }

            String responseBody = httpResponse.body().string();
            
            // Parse the response manually
            String content = extractContentFromResponse(responseBody);
            return content;

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return "I encountered an error while processing your request. Please try again.";
        }
    }

    /**
     * Extract content from OpenAI API response
     */
    private String extractContentFromResponse(String responseJson) {
        try {
            // Simple JSON parsing to extract the assistant's response
            int contentIndex = responseJson.indexOf("\"content\":");
            if (contentIndex == -1) return "No response received";
            
            int startIndex = responseJson.indexOf("\"", contentIndex + 10) + 1;
            int endIndex = responseJson.indexOf("\"", startIndex);
            
            return responseJson.substring(startIndex, endIndex)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            log.error("Error parsing OpenAI response", e);
            return "Unable to parse response.";
        }
    }

    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        return "\"" + str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    /**
     * Get auto-suggested questions for a lesson
     */
    public List<String> getSuggestedQuestions(Long lessonId) {
        List<String> suggestions = new ArrayList<>();
        
        // Generic suggestions based on lesson type
        suggestions.add("What are the main concepts in this lesson?");
        suggestions.add("Can you explain this more simply?");
        suggestions.add("How can I apply this concept?");
        suggestions.add("What are some real-world examples?");
        suggestions.add("What are the prerequisites for understanding this?");
        
        return suggestions;
    }

    /**
     * Generate summary for a lesson
     */
    @Transactional
    public LessonSummaryDTO generateSummary(Long lessonId) {
        try {
            long startTime = System.currentTimeMillis();

            // Check if summary already exists in cache
            Optional<LessonSummary> existingSummary = lessonSummaryRepository.findByLessonId(lessonId);
            if (existingSummary.isPresent()) {
                LessonSummary summary = existingSummary.get();
                // Increment view count
                summary.incrementViewCount();
                lessonSummaryRepository.save(summary);
                
                return convertToDTO(summary, true);
            }

            // Fetch lesson
            Optional<Resource> lesson = resourceRepository.findById(lessonId);
            if (lesson.isEmpty()) {
                log.error("Lesson not found: {}", lessonId);
                throw new RuntimeException("Lesson not found");
            }

            // Build summary prompt
            String prompt = buildSummaryPrompt(lesson.get());

            // Call OpenAI
            String summary = callOpenAIAPI(prompt, 1500, 0.3);

            // Parse key points from summary
            List<String> keyPoints = extractKeyPoints(summary);

            // Save summary to database
            LessonSummary newSummary = new LessonSummary();
            newSummary.setLesson(lesson.get());
            newSummary.setContent(summary);
            newSummary.setKeyPoints(serializeKeyPoints(keyPoints));
            newSummary.setViewCount(1);
            LessonSummary savedSummary = lessonSummaryRepository.save(newSummary);

            long generationTime = System.currentTimeMillis() - startTime;
            log.info("Generated summary for lesson {} in {} ms", lessonId, generationTime);

            return convertToDTO(savedSummary, false);

        } catch (Exception e) {
            log.error("Error generating summary for lesson {}", lessonId, e);
            throw new RuntimeException("Error generating summary: " + e.getMessage());
        }
    }

    /**
     * Build summary prompt from lesson
     */
    private String buildSummaryPrompt(Resource lesson) {
        return "You are an expert educational content summarizer. Please read the following lesson content and provide a comprehensive summary.\n\n" +
                "LESSON TITLE: " + lesson.getTitle() + "\n" +
                "LESSON CONTENT/REFERENCE: " + lesson.getUrl() + "\n\n" +
                "Please provide:\n" +
                "1. A brief 2-3 paragraph summary of the main concepts\n" +
                "2. A bulleted list of key points (at least 5-7 important takeaways)\n" +
                "3. Any important definitions or formulas\n\n" +
                "Format the response clearly with sections separated by blank lines. " +
                "Make it easy to scan and understand.";
    }

    /**
     * Extract key points from summary text
     */
    private List<String> extractKeyPoints(String summaryText) {
        List<String> keyPoints = new ArrayList<>();
        
        // Split by newline and look for bullet points or numbered items
        String[] lines = summaryText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // Look for lines starting with -, *, or numbers
            if (trimmed.matches("^[-*•]\\s+.*") || trimmed.matches("^\\d+\\.\\s+.*")) {
                // Remove the bullet/number and add to list
                String point = trimmed.replaceAll("^[-*•]\\s+", "").replaceAll("^\\d+\\.\\s+", "");
                if (!point.isEmpty() && point.length() > 10) {
                    keyPoints.add(point);
                }
            }
        }
        
        // If no bullet points found, split summary into sentences for key points
        if (keyPoints.isEmpty()) {
            String[] sentences = summaryText.split("\\.");
            for (String sentence : sentences) {
                String trimmed = sentence.trim();
                if (trimmed.length() > 20 && trimmed.length() < 200) {
                    keyPoints.add(trimmed + ".");
                    if (keyPoints.size() >= 5) break;
                }
            }
        }
        
        return keyPoints.isEmpty() ? Arrays.asList(summaryText) : keyPoints;
    }

    /**
     * Serialize key points to JSON string
     */
    private String serializeKeyPoints(List<String> keyPoints) {
        try {
            return objectMapper.writeValueAsString(keyPoints);
        } catch (Exception e) {
            log.error("Error serializing key points", e);
            return "[]";
        }
    }

    /**
     * Deserialize key points from JSON string
     */
    private List<String> deserializeKeyPoints(String keyPointsJson) {
        try {
            return objectMapper.readValue(keyPointsJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Error deserializing key points", e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert LessonSummary entity to DTO
     */
    private LessonSummaryDTO convertToDTO(LessonSummary summary, boolean isFromCache) {
        List<String> keyPoints = deserializeKeyPoints(summary.getKeyPoints());
        
        return LessonSummaryDTO.builder()
                .id(summary.getId())
                .lessonId(summary.getLesson().getId())
                .content(summary.getContent())
                .keyPoints(keyPoints)
                .createdAt(summary.getCreatedAt())
                .updatedAt(summary.getUpdatedAt())
                .viewCount(summary.getViewCount())
                .isFromCache(isFromCache)
                .build();
    }
}
