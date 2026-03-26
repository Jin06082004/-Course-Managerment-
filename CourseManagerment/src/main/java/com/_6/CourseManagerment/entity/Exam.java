package com._6.CourseManagerment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "exams", indexes = {
        @Index(name = "idx_course_id", columnList = "course_id"),
        @Index(name = "idx_instructor_id", columnList = "instructor_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Column(nullable = false)
    private Integer duration;  // in minutes

    @Column(nullable = false)
    private Integer totalMarks;

    @Column(nullable = false)
    private Integer passingMarks;

    @Column(nullable = false)
    @Default
    private Boolean isPublished = false;

    @Column(nullable = false)
    @Default
    private Boolean isDraft = true;

    @Column(nullable = false)
    @Default
    private Boolean randomizeQuestions = false;

    @Column(nullable = false)
    @Default
    private Boolean showResults = true;

    @Column(nullable = false)
    @Default
    private Integer questionCount = 0;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamQuestion> questions;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamResult> results;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void publish() {
        this.isPublished = true;
        this.isDraft = false;
        this.publishedAt = LocalDateTime.now();
    }

    public void saveDraft() {
        this.isPublished = false;
        this.isDraft = true;
        this.publishedAt = null;
    }

    public void updateQuestionCount() {
        this.questionCount = questions != null ? questions.size() : 0;
    }
}
