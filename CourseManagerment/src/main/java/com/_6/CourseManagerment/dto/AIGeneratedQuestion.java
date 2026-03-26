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
public class AIGeneratedQuestion {
    private String question;
    private List<String> options;        // [A, B, C, D]
    private String correctAnswer;        // A, B, C, or D
    private String explanation;          // Why this is correct
}
