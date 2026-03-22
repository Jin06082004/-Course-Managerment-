package com._6.CourseManagerment.dto;

import com._6.CourseManagerment.entity.Wishlist;
import java.time.LocalDateTime;

/**
 * DTO for wishlist item response
 */
public class WishlistDto {

    private Long id;
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private String courseThumbnail;
    private String courseLevel;
    private String instructorName;
    private Float rating;
    private Integer studentCount;
    private String thumbnailUrl;
    private LocalDateTime addedAt;

    public WishlistDto() {}

    public WishlistDto(Wishlist wishlist) {
        this.id = wishlist.getId();
        this.userId = wishlist.getUser().getId();
        this.courseId = wishlist.getCourse().getId();
        this.courseTitle = wishlist.getCourse().getTitle();
        this.courseLevel = wishlist.getCourse().getLevel();
        this.rating = wishlist.getCourse().getRating();
        this.studentCount = wishlist.getCourse().getStudentCount();
        this.thumbnailUrl = wishlist.getCourse().getThumbnailUrl();
        this.addedAt = wishlist.getCreatedAt();
        this.instructorName = wishlist.getCourse().getInstructor() != null
            ? wishlist.getCourse().getInstructor().getFullName()
            : "Unknown";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getCourseThumbnail() { return courseThumbnail; }
    public void setCourseThumbnail(String courseThumbnail) { this.courseThumbnail = courseThumbnail; }
    public String getCourseLevel() { return courseLevel; }
    public void setCourseLevel(String courseLevel) { this.courseLevel = courseLevel; }
    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    public Float getRating() { return rating; }
    public void setRating(Float rating) { this.rating = rating; }
    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
