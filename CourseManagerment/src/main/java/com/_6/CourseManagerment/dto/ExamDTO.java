package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDTO {
    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private Long instructorId;
    private Integer duration;  // minutes
    private Integer totalMarks;
    private Integer passingMarks;
    private Boolean isPublished;
    private Boolean isDraft;
    private Boolean randomizeQuestions;
    private Boolean showResults;
    private Integer questionCount;
    private List<ExamQuestionDTO> questions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
