package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendationResponse {
    private Boolean success;
    private String message;
    private String error;
    private List<RecommendedCourse> recommendations;
    private Integer recommendationCount;
    private String analysisReason;      // Why these recommendations are given
}
