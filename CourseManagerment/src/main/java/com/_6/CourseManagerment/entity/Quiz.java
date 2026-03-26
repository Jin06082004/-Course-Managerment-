package com._6.CourseManagerment.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Quiz {

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
    @JoinColumn(name = "lesson_id")
    private Resource lesson;  // Optional: attached to specific lesson

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User instructor;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<QuizQuestion> questions;

    @Column(nullable = false)
    @Default
    private Integer passingScore = 70;  // Percentage needed to pass

    @Column(nullable = false)
    @Default
    private Boolean isPublished = false;

    @Column(nullable = false)
    @Default
    private Boolean isDraft = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    @Default
    private Integer questionCount = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateQuestionCount() {
        this.questionCount = this.questions != null ? this.questions.size() : 0;
    }

    public void publish() {
        this.isPublished = true;
        this.isDraft = false;
        this.publishedAt = LocalDateTime.now();
    }

    public void saveDraft() {
        this.isPublished = false;
        this.isDraft = true;
    }
}
