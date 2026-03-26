package com._6.CourseManagerment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_results", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_exam_id", columnList = "exam_id"),
        @Index(name = "idx_user_exam", columnList = "user_id, exam_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(nullable = false)
    private Integer score;  // marks obtained

    @Column(nullable = false)
    private Integer totalMarks;

    @Column(nullable = false)
    private Integer percentage;

    @Column(nullable = false)
    private Boolean passed;

    @Column(columnDefinition = "LONGTEXT")
    private String answersJson;  // JSON of user answers

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        if (totalMarks != null && totalMarks > 0) {
            percentage = Math.round((score * 100.0f) / totalMarks);
            passed = percentage >= (exam != null ? exam.getPassingMarks() : 50);
        }
    }
}
