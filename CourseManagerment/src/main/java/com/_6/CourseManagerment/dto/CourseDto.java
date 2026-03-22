package com._6.CourseManagerment.dto;

import com._6.CourseManagerment.entity.Course;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CourseDto {
    
    private Long id;
    private String title;
    private String description;
    private String code;
    private Long categoryId;
    private String categoryName;
    private Long instructorId;
    private String instructorName;
    private String level;
    private BigDecimal price;
    private Integer duration;
    private Float rating;
    private Integer studentCount;
    private String thumbnailUrl;
    private String videoUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public CourseDto() {}
    
    public CourseDto(Course course) {
        this.id = course.getId();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.code = course.getCode();
        this.categoryId = course.getCategory() != null ? course.getCategory().getId() : null;
        this.categoryName = course.getCategory() != null ? course.getCategory().getName() : null;
        this.instructorId = course.getInstructor() != null ? course.getInstructor().getId() : null;
        this.instructorName = course.getInstructor() != null ? course.getInstructor().getFullName() : null;
        this.level = course.getLevel();
        this.price = course.getPrice();
        this.duration = course.getDuration();
        this.rating = course.getRating();
        this.studentCount = course.getStudentCount();
        this.thumbnailUrl = course.getThumbnailUrl();
        this.videoUrl = course.getVideoUrl();
        this.status = course.getStatus();
        this.createdAt = course.getCreatedAt();
        this.updatedAt = course.getUpdatedAt();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public Long getInstructorId() {
        return instructorId;
    }
    
    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }
    
    public String getInstructorName() {
        return instructorName;
    }
    
    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public Float getRating() {
        return rating;
    }
    
    public void setRating(Float rating) {
        this.rating = rating;
    }
    
    public Integer getStudentCount() {
        return studentCount;
    }
    
    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }
    
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
    
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
    
    public String getVideoUrl() {
        return videoUrl;
    }
    
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
