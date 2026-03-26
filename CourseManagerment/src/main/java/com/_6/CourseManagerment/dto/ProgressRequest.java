package com._6.CourseManagerment.dto;

import jakarta.validation.constraints.NotNull;

public class ProgressRequest {

    @NotNull(message = "lessonId is required")
    private Long lessonId;

    @NotNull(message = "currentTime is required")
    private Double currentTime;

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public Double getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Double currentTime) {
        this.currentTime = currentTime;
    }
}
