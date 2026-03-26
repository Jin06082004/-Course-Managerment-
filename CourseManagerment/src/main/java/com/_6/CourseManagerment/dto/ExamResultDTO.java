package com._6.CourseManagerment.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResultDTO {
    private Long id;
    private Long examId;
    private Long userId;
    private Integer score;
    private Integer totalMarks;
    private Integer percentage;
    private Boolean passed;
    private String answersJson;  // Raw JSON of answers
    private Map<Long, Object> submissionDetails;  // Parsed submission details with answer tracking
    private LocalDateTime submittedAt;
    private ExamDTO exam;  // include exam details

    /**
     * Parse submission details from JSON
     */
    public void parseSubmissionDetails() {
        if (answersJson != null && !answersJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                submissionDetails = mapper.readValue(answersJson, new TypeReference<Map<Long, Object>>() {});
            } catch (Exception e) {
                // Skip parsing errors
                submissionDetails = null;
            }
        }
    }
}
