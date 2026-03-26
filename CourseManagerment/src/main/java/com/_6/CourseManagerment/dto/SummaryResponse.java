package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryResponse {
    private Boolean success;
    private String message;
    private LessonSummaryDTO summary;
    private String error;
    private Long generationTimeMs;
}
