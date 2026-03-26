package com._6.CourseManagerment.dto;

import com._6.CourseManagerment.entity.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDto {

    private Long id;
    private Long userId;
    private String username;
    private Long lessonId;
    private String content;
    private LocalDateTime createdAt;
    private Long parentId;
    private int likeCount;
    private boolean liked;
    private List<CommentDto> replies = new ArrayList<>();

    public CommentDto() {}

    public CommentDto(Comment comment, String username, Long currentUserId) {
        this.id = comment.getId();
        this.userId = comment.getUserId();
        this.username = username;
        this.lessonId = comment.getLessonId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.parentId = comment.getParentId();
        this.likeCount = comment.getLikedUserIds() == null ? 0 : comment.getLikedUserIds().size();
        this.liked = currentUserId != null && comment.getLikedUserIds() != null && comment.getLikedUserIds().contains(currentUserId);
    }

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public List<CommentDto> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentDto> replies) {
        this.replies = replies;
    }
}
