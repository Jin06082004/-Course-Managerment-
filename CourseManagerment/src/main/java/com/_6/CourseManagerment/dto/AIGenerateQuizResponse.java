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
public class AIGenerateQuizResponse {
    private Boolean success;
    private String message;
    private String error;
    private List<AIGeneratedQuestion> questions;
    private Integer questionsCount;
}
