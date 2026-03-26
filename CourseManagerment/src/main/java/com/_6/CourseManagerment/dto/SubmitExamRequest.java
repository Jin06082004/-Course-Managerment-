package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitExamRequest {
    private Long examId;
    private Map<Long, String> answers;  // questionId -> answer (A/B/C/D)
    private Long submittedInSeconds;    // time taken
}
