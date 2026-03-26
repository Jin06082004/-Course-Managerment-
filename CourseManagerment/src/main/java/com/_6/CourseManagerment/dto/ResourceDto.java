package com._6.CourseManagerment.dto;

import com._6.CourseManagerment.entity.Resource;

public class ResourceDto {

    private Long id;
    private String title;
    private String url;
    private Long courseId;

    public ResourceDto() {}

    public ResourceDto(Resource resource) {
        this.id = resource.getId();
        this.title = resource.getTitle();
        this.url = resource.getUrl();
        this.courseId = resource.getCourse() != null ? resource.getCourse().getId() : null;
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
