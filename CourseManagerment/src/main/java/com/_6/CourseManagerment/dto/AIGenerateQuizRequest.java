package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIGenerateQuizRequest {
    private Long lessonId;
    private Integer numberOfQuestions;    // 3-10 questions
    private String quizTitle;             // Optional custom title
    private String quizDescription;       // Optional custom description
}
