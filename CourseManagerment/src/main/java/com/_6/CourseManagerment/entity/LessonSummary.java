package com._6.CourseManagerment.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_summaries", indexes = {
    @Index(name = "idx_lesson", columnList = "lesson_id"),
    @Index(name = "idx_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LessonSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Resource lesson;

    @Column(name = "summary_content", columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "key_points", columnDefinition = "LONGTEXT")
    private String keyPoints;  // JSON array of key points

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount = this.viewCount == null ? 1 : this.viewCount + 1;
    }
}
