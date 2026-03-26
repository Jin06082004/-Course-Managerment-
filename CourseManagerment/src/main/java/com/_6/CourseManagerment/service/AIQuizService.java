package com._6.CourseManagerment.service;

import com._6.CourseManagerment.config.OpenAIProperties;
import com._6.CourseManagerment.dto.AIGenerateQuizRequest;
import com._6.CourseManagerment.dto.AIGenerateQuizResponse;
import com._6.CourseManagerment.dto.AIGeneratedQuestion;
import com._6.CourseManagerment.entity.Resource;
import com._6.CourseManagerment.repository.ResourceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIQuizService {

    private final ResourceRepository resourceRepository;
    private final OpenAIProperties openAIProperties;
    private final ObjectMapper objectMapper;

    /**
     * Generate quiz questions using OpenAI based on lesson content
     */
    public AIGenerateQuizResponse generateQuiz(AIGenerateQuizRequest request) {
        try {
            // Validate input
            if (request.getNumberOfQuestions() == null || request.getNumberOfQuestions() < 3 || request.getNumberOfQuestions() > 10) {
                return AIGenerateQuizResponse.builder()
                        .success(false)
                        .error("Number of questions must be between 3 and 10")
                        .build();
            }

            // Get lesson content
            Optional<Resource> lesson = resourceRepository.findById(request.getLessonId());
            if (lesson.isEmpty()) {
                log.warn("Lesson not found: {}", request.getLessonId());
                return AIGenerateQuizResponse.builder()
                        .success(false)
                        .error("Lesson not found")
                        .build();
            }

            String lessonContent = lesson.get().getTitle() + ". " + lesson.get().getUrl();
            
            // Build prompt for AI
            String prompt = buildQuizGenerationPrompt(lessonContent, request.getNumberOfQuestions());

            // Call OpenAI API
            String aiResponse = callOpenAIAPI(prompt, 3000, 0.7);

            if (aiResponse == null || aiResponse.isEmpty()) {
                return AIGenerateQuizResponse.builder()
                        .success(false)
                        .error("Failed to generate questions from AI")
                        .build();
            }

            // Parse AI response to extract questions
            List<AIGeneratedQuestion> questions = parseAIResponse(aiResponse);

            if (questions == null || questions.isEmpty()) {
                return AIGenerateQuizResponse.builder()
                        .success(false)
                        .error("Failed to parse generated questions")
                        .build();
            }

            log.info("Successfully generated {} quiz questions for lesson: {}", questions.size(), request.getLessonId());

            return AIGenerateQuizResponse.builder()
                    .success(true)
                    .message("Quiz questions generated successfully")
                    .questions(questions)
                    .questionsCount(questions.size())
                    .build();

        } catch (Exception e) {
            log.error("Error generating quiz with AI", e);
            return AIGenerateQuizResponse.builder()
                    .success(false)
                    .error("Error generating quiz: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Build prompt for quiz generation
     */
    private String buildQuizGenerationPrompt(String lessonContent, Integer numberOfQuestions) {
        return "Based on the following lesson content, generate exactly " + numberOfQuestions + " multiple choice questions with 4 options each (A, B, C, D).\n\n" +
                "Lesson Content:\n" + lessonContent + "\n\n" +
                "For each question, provide in strict JSON format (no markdown, just pure JSON array):\n" +
                "[\n" +
                "  {\n" +
                "    \"question\": \"Question text here?\",\n" +
                "    \"options\": [\"Option A text\", \"Option B text\", \"Option C text\", \"Option D text\"],\n" +
                "    \"correctAnswer\": \"A\",\n" +
                "    \"explanation\": \"Why this is the correct answer\"\n" +
                "  }\n" +
                "]\n\n" +
                "Important:\n" +
                "1. Generate EXACTLY " + numberOfQuestions + " questions\n" +
                "2. Each question must have 4 unique options\n" +
                "3. correctAnswer must be A, B, C, or D\n" +
                "4. Return ONLY valid JSON array, no other text\n" +
                "5. Make questions clear and educational";
    }

    /**
     * Call OpenAI API
     */
    private String callOpenAIAPI(String prompt, Integer maxTokens, Double temperature) {
        try {
            String apiKey = openAIProperties.getApiKey();
            String model = openAIProperties.getModel() != null ? openAIProperties.getModel() : "gpt-3.5-turbo";

            if (maxTokens == null) {
                maxTokens = openAIProperties.getMaxTokens() != null ? openAIProperties.getMaxTokens() : 3000;
            }
            if (temperature == null) {
                temperature = openAIProperties.getTemperature() != null ? openAIProperties.getTemperature() : 0.7;
            }

            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("OpenAI API key not configured");
                return null;
            }

            // Create JSON payload
            String payload = "{" +
                    "\"model\": \"" + model + "\"," +
                    "\"messages\": [{\"role\": \"user\", \"content\": " + escapeJson(prompt) + "}]," +
                    "\"max_tokens\": " + maxTokens + "," +
                    "\"temperature\": " + temperature +
                    "}";

            // Make HTTP request to OpenAI
            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(
                    okhttp3.MediaType.parse("application/json"),
                    payload
            );

            Request httpRequest = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            Response httpResponse = client.newCall(httpRequest).execute();

            if (!httpResponse.isSuccessful()) {
                log.error("OpenAI API error: {}", httpResponse.code());
                return null;
            }

            String responseBody = httpResponse.body().string();

            // Extract content from response
            return extractContentFromOpenAIResponse(responseBody);

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return null;
        }
    }

    /**
     * Extract content from OpenAI API response
     */
    private String extractContentFromOpenAIResponse(String response) {
        try {
            var jsonNode = objectMapper.readTree(response);
            var choices = jsonNode.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                var content = choices.get(0).get("message").get("content");
                if (content != null) {
                    return content.asText();
                }
            }
        } catch (Exception e) {
            log.error("Error extracting content from OpenAI response", e);
        }
        return null;
    }

    /**
     * Parse AI response to extract questions
     */
    private List<AIGeneratedQuestion> parseAIResponse(String aiResponse) {
        try {
            // Clean response - remove markdown code blocks if present
            String cleanResponse = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            // Try to find JSON array in response
            int startIdx = cleanResponse.indexOf('[');
            int endIdx = cleanResponse.lastIndexOf(']');

            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonPart = cleanResponse.substring(startIdx, endIdx + 1);
                List<AIGeneratedQuestion> questions = objectMapper.readValue(
                        jsonPart,
                        new TypeReference<List<AIGeneratedQuestion>>() {}
                );
                return questions;
            }
        } catch (Exception e) {
            log.error("Error parsing AI response to JSON", e);
        }
        return new ArrayList<>();
    }

    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "\"\"";
        }
        return "\"" + input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
