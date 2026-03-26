package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIChatResponse {
    private Long id;
    private String userMessage;
    private String aiResponse;
    private LocalDateTime timestamp;
    private String senderType;
    private Boolean success;
    private String error;
}
