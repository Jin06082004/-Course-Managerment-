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
public class ExamListResponse {
    private Boolean success;
    private String message;
    private List<ExamDTO> exams;
    private List<ExamResultDTO> results;  // For exam results response
    private String error;
}
