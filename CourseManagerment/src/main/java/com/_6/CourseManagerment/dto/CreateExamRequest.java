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
public class CreateExamRequest {
    private String title;
    private String description;
    private Long courseId;
    private Integer duration;  // minutes
    private Integer totalMarks;
    private Integer passingMarks;
    private Boolean randomizeQuestions;
    private Boolean showResults;
    private Boolean saveAsDraft;
    private List<ExamQuestionDTO> questions;
}
