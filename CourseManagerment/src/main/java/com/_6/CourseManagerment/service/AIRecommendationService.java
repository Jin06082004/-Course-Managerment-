package com._6.CourseManagerment.service;

import com._6.CourseManagerment.config.OpenAIProperties;
import com._6.CourseManagerment.dto.AIRecommendationResponse;
import com._6.CourseManagerment.dto.RecommendedCourse;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.Enrollment;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import com._6.CourseManagerment.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AIRecommendationService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OpenAIProperties openAIProperties;
    private final ObjectMapper objectMapper;

    /**
     * Get AI-powered course recommendations for a user
     */
    public AIRecommendationResponse getRecommendations(Long userId) {
        try {
            // Check if user exists
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return AIRecommendationResponse.builder()
                        .success(false)
                        .error("User not found")
                        .build();
            }

            User user = userOpt.get();

            // Get user's learning profile
            List<Enrollment> enrolledCourses = enrollmentRepository.findActiveEnrollments(
                    userId, 
                    PageRequest.of(0, 100)
            ).getContent();

            List<Enrollment> completedCourses = enrollmentRepository.findCompletedEnrollments(
                    userId, 
                    PageRequest.of(0, 100)
            ).getContent();

            // Extract categories and levels the user is interested in
            Set<String> interestedCategories = extractCategories(enrolledCourses, completedCourses);
            Set<String> enrolledCourseIds = extractCourseIds(enrolledCourses, completedCourses);

            // Get published courses not yet enrolled
            List<Course> availableCourses = courseRepository.findByStatus("PUBLISHED", PageRequest.of(0, 100))
                    .getContent()
                    .stream()
                    .filter(c -> !enrolledCourseIds.contains(c.getId().toString()))
                    .collect(Collectors.toList());

            if (availableCourses.isEmpty()) {
                return AIRecommendationResponse.builder()
                        .success(true)
                        .message("No available courses to recommend")
                        .recommendations(new ArrayList<>())
                        .recommendationCount(0)
                        .build();
            }

            // Build user profile for AI
            String userProfile = buildUserProfile(user, enrolledCourses, completedCourses, interestedCategories);

            // Build course catalog for AI
            String courseCatalog = buildCourseCatalog(availableCourses);

            // Call OpenAI to generate recommendations
            String aiResponse = callOpenAIForRecommendations(userProfile, courseCatalog);

            if (aiResponse == null || aiResponse.isEmpty()) {
                return AIRecommendationResponse.builder()
                        .success(false)
                        .error("Failed to generate recommendations from AI")
                        .build();
            }

            // Parse AI response to extract recommendations
            List<RecommendedCourse> recommendations = parseAIRecommendations(aiResponse, availableCourses);

            if (recommendations == null || recommendations.isEmpty()) {
                return AIRecommendationResponse.builder()
                        .success(false)
                        .error("Failed to parse AI recommendations")
                        .build();
            }

            // Sort by relevance score
            recommendations.sort((a, b) -> b.getRelevanceScore().compareTo(a.getRelevanceScore()));

            // Limit to top 10 recommendations
            if (recommendations.size() > 10) {
                recommendations = recommendations.subList(0, 10);
            }

            log.info("Generated {} recommendations for user: {}", recommendations.size(), userId);

            return AIRecommendationResponse.builder()
                    .success(true)
                    .message("Recommendations generated successfully")
                    .recommendations(recommendations)
                    .recommendationCount(recommendations.size())
                    .analysisReason("Based on your learning history, progress, and interests")
                    .build();

        } catch (Exception e) {
            log.error("Error generating recommendations for user: {}", userId, e);
            return AIRecommendationResponse.builder()
                    .success(false)
                    .error("Error generating recommendations: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Extract interested categories from user's enrollments
     */
    private Set<String> extractCategories(List<Enrollment> enrolled, List<Enrollment> completed) {
        Set<String> categories = new java.util.HashSet<>();
        
        enrolled.forEach(e -> {
            if (e.getCourse() != null && e.getCourse().getCategory() != null) {
                categories.add(e.getCourse().getCategory().getName());
            }
        });
        
        completed.forEach(e -> {
            if (e.getCourse() != null && e.getCourse().getCategory() != null) {
                categories.add(e.getCourse().getCategory().getName());
            }
        });
        
        return categories;
    }

    /**
     * Extract enrolled course IDs
     */
    private Set<String> extractCourseIds(List<Enrollment> enrolled, List<Enrollment> completed) {
        Set<String> courseIds = new java.util.HashSet<>();
        
        enrolled.forEach(e -> courseIds.add(e.getCourse().getId().toString()));
        completed.forEach(e -> courseIds.add(e.getCourse().getId().toString()));
        
        return courseIds;
    }

    /**
     * Build user profile for AI analysis
     */
    private String buildUserProfile(User user, List<Enrollment> enrolled, List<Enrollment> completed, Set<String> categories) {
        StringBuilder profile = new StringBuilder();
        profile.append("User Profile:\n");
        profile.append("- Name: ").append(user.getFullName()).append("\n");
        profile.append("- Active Courses: ").append(enrolled.size()).append("\n");
        profile.append("- Completed Courses: ").append(completed.size()).append("\n");
        profile.append("- Interested Categories: ").append(String.join(", ", categories)).append("\n");
        
        // Calculate average progress
        float avgProgress = (float) enrolled.stream()
                .mapToDouble(e -> e.getProgressPercentage() != null ? e.getProgressPercentage() : 0)
                .average()
                .orElse(0);
        profile.append("- Average Progress: ").append(String.format("%.1f%%", avgProgress)).append("\n");
        
        // Learning levels
        Set<String> levels = new java.util.HashSet<>();
        enrolled.forEach(e -> levels.add(e.getCourse().getLevel()));
        completed.forEach(e -> levels.add(e.getCourse().getLevel()));
        profile.append("- Learning Levels: ").append(String.join(", ", levels)).append("\n");
        
        return profile.toString();
    }

    /**
     * Build course catalog for AI analysis
     */
    private String buildCourseCatalog(List<Course> courses) {
        StringBuilder catalog = new StringBuilder();
        catalog.append("Available Courses:\n");
        
        for (int i = 0; i < Math.min(courses.size(), 30); i++) {
            Course c = courses.get(i);
            catalog.append(String.format(
                    "%d. [%s] %s (Category: %s, Level: %s, Rating: %.1f/5, Students: %d)\n",
                    i + 1,
                    c.getLevel(),
                    c.getTitle(),
                    c.getCategory() != null ? c.getCategory().getName() : "Unknown",
                    c.getLevel(),
                    c.getRating() != null ? c.getRating() : 0,
                    c.getStudentCount() != null ? c.getStudentCount() : 0
            ));
        }
        
        return catalog.toString();
    }

    /**
     * Call OpenAI to generate recommendations
     */
    private String callOpenAIForRecommendations(String userProfile, String courseCatalog) {
        try {
            String apiKey = openAIProperties.getApiKey();
            String model = openAIProperties.getModel() != null ? openAIProperties.getModel() : "gpt-3.5-turbo";

            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("OpenAI API key not configured");
                return null;
            }

            String prompt = buildRecommendationPrompt(userProfile, courseCatalog);

            String payload = "{" +
                    "\"model\": \"" + model + "\"," +
                    "\"messages\": [{\"role\": \"user\", \"content\": " + escapeJson(prompt) + "}]," +
                    "\"max_tokens\": 2000," +
                    "\"temperature\": 0.7" +
                    "}";

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
            return extractContentFromOpenAIResponse(responseBody);

        } catch (Exception e) {
            log.error("Error calling OpenAI API for recommendations", e);
            return null;
        }
    }

    /**
     * Build recommendation prompt
     */
    private String buildRecommendationPrompt(String userProfile, String courseCatalog) {
        return "You are an intelligent course recommendation system. Based on the user's learning profile and available courses, " +
                "recommend the 5-10 most suitable courses for this learner.\n\n" +
                userProfile + "\n" +
                courseCatalog + "\n" +
                "Based on the user profile, analyze their interests, learning level, and progress, then recommend the most relevant courses. " +
                "Return ONLY a valid JSON array with no markdown formatting or extra text:\n" +
                "[\n" +
                "  {\n" +
                "    \"courseTitle\": \"Exact course title from catalog\",\n" +
                "    \"reason\": \"Why this course is recommended for this user\",\n" +
                "    \"relevanceScore\": 95\n" +
                "  }\n" +
                "]\n\n" +
                "Important:\n" +
                "1. Only recommend courses from the provided catalog\n" +
                "2. Match EXACT course titles\n" +
                "3. relevanceScore should be 0-100\n" +
                "4. Return ONLY valid JSON array, no other text\n" +
                "5. Provide 5-10 recommendations";
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
     * Parse AI recommendations response
     */
    private List<RecommendedCourse> parseAIRecommendations(String aiResponse, List<Course> availableCourses) {
        try {
            String cleanResponse = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            int startIdx = cleanResponse.indexOf('[');
            int endIdx = cleanResponse.lastIndexOf(']');

            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonPart = cleanResponse.substring(startIdx, endIdx + 1);
                
                List<java.util.Map<String, Object>> parsedData = objectMapper.readValue(
                        jsonPart,
                        new TypeReference<List<java.util.Map<String, Object>>>() {}
                );

                List<RecommendedCourse> result = new ArrayList<>();

                for (var item : parsedData) {
                    String courseTitle = (String) item.get("courseTitle");
                    String reason = (String) item.get("reason");
                    Number score = (Number) item.get("relevanceScore");
                    
                    // Find matching course
                    Course matchedCourse = availableCourses.stream()
                            .filter(c -> c.getTitle().equalsIgnoreCase(courseTitle))
                            .findFirst()
                            .orElse(null);

                    if (matchedCourse != null) {
                        result.add(RecommendedCourse.builder()
                                .id(matchedCourse.getId())
                                .title(matchedCourse.getTitle())
                                .description(matchedCourse.getDescription())
                                .category(matchedCourse.getCategory() != null ? matchedCourse.getCategory().getName() : "")
                                .level(matchedCourse.getLevel())
                                .price(matchedCourse.getPrice())
                                .duration(matchedCourse.getDuration())
                                .rating(matchedCourse.getRating())
                                .studentCount(matchedCourse.getStudentCount())
                                .thumbnailUrl(matchedCourse.getThumbnailUrl())
                                .reason(reason)
                                .relevanceScore(score != null ? score.floatValue() : 0f)
                                .build());
                    }
                }

                return result;
            }
        } catch (Exception e) {
            log.error("Error parsing AI recommendations", e);
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
