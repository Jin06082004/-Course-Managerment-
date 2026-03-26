package com._6.CourseManagerment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedCourse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String level;           // BEGINNER, INTERMEDIATE, ADVANCED
    private BigDecimal price;
    private Integer duration;       // in hours
    private Float rating;
    private Integer studentCount;
    private String thumbnailUrl;
    private String reason;          // Why this course is recommended
    private Float relevanceScore;   // 0-100, how relevant to user
}
