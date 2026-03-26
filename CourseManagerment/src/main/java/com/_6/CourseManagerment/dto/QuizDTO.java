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
public class QuizDTO {
    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private Long lessonId;
    private Long instructorId;
    private List<QuizQuestionDto> questions;
    private Integer passingScore;
    private Boolean isPublished;
    private Boolean isDraft;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private Integer questionCount;
}
