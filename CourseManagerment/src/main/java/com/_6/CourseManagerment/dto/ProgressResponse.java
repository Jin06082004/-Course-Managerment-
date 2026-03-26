package com._6.CourseManagerment.dto;

public class ProgressResponse {

    private Long lessonId;
    private Double lastWatchedTime;

    public ProgressResponse() {}

    public ProgressResponse(Long lessonId, Double lastWatchedTime) {
        this.lessonId = lessonId;
        this.lastWatchedTime = lastWatchedTime;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public Double getLastWatchedTime() {
        return lastWatchedTime;
    }

    public void setLastWatchedTime(Double lastWatchedTime) {
        this.lastWatchedTime = lastWatchedTime;
    }
}
