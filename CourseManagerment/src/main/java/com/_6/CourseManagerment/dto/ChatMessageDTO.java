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
public class ChatMessageDTO {
    private Long id;
    private String content;
    private String aiResponse;
    private String senderType;
    private LocalDateTime createdAt;
    private Long userId;
    private Long lessonId;
}
