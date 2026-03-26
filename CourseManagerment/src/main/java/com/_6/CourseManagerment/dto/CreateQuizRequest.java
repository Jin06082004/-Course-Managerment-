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
public class CreateQuizRequest {
    private String title;
    private String description;
    private Long courseId;
    private Long lessonId;  // Optional
    private Integer passingScore;
    private Boolean saveAsDraft;  // true = draft, false = publish
    private List<QuizQuestionDto> questions;
}
