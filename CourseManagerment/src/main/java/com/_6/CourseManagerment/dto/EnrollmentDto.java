package com._6.CourseManagerment.dto;

import com._6.CourseManagerment.entity.Enrollment;
import java.time.LocalDateTime;

public class EnrollmentDto {
    
    private Long id;
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private String courseThumbnail;
    private String courseLevel;
    private LocalDateTime enrollmentDate;
    private LocalDateTime completionDate;
    private Float progressPercentage;
    private String status; // ENROLLED, IN_PROGRESS, COMPLETED
    private LocalDateTime lastAccessedDate;
    private String instructorName;
    private Integer totalLessons;
    
    public EnrollmentDto() {}
    
    public EnrollmentDto(Enrollment enrollment) {
        this.id = enrollment.getId();
        this.userId = enrollment.getUser().getId();
        this.courseId = enrollment.getCourse().getId();
        this.courseTitle = enrollment.getCourse().getTitle();
        this.courseThumbnail = enrollment.getCourse().getThumbnailUrl();
        this.courseLevel = enrollment.getCourse().getLevel();
        this.enrollmentDate = enrollment.getEnrollmentDate();
        this.completionDate = enrollment.getCompletionDate();
        this.progressPercentage = enrollment.getProgressPercentage();
        this.status = enrollment.getStatus();
        this.lastAccessedDate = enrollment.getLastAccessedDate();
        this.instructorName = enrollment.getCourse().getInstructor() != null ? 
                            enrollment.getCourse().getInstructor().getFullName() : "Unknown";
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getCourseId() {
        return courseId;
    }
    
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
    
    public String getCourseTitle() {
        return courseTitle;
    }
    
    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }
    
    public String getCourseThumbnail() {
        return courseThumbnail;
    }
    
    public void setCourseThumbnail(String courseThumbnail) {
        this.courseThumbnail = courseThumbnail;
    }
    
    public String getCourseLevel() {
        return courseLevel;
    }
    
    public void setCourseLevel(String courseLevel) {
        this.courseLevel = courseLevel;
    }
    
    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }
    
    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    
    public LocalDateTime getCompletionDate() {
        return completionDate;
    }
    
    public void setCompletionDate(LocalDateTime completionDate) {
        this.completionDate = completionDate;
    }
    
    public Float getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Float progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getLastAccessedDate() {
        return lastAccessedDate;
    }
    
    public void setLastAccessedDate(LocalDateTime lastAccessedDate) {
        this.lastAccessedDate = lastAccessedDate;
    }
    
    public String getInstructorName() {
        return instructorName;
    }
    
    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }
    
    public Integer getTotalLessons() {
        return totalLessons;
    }
    
    public void setTotalLessons(Integer totalLessons) {
        this.totalLessons = totalLessons;
    }
}
