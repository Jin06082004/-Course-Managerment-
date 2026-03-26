package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.LessonSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LessonSummaryRepository extends JpaRepository<LessonSummary, Long> {

    @Query("SELECT s FROM LessonSummary s WHERE s.lesson.id = :lessonId")
    Optional<LessonSummary> findByLessonId(@Param("lessonId") Long lessonId);

    @Query("SELECT COUNT(s) FROM LessonSummary s WHERE s.lesson.id = :lessonId")
    boolean existsByLessonId(@Param("lessonId") Long lessonId);

    @Query("UPDATE LessonSummary s SET s.viewCount = s.viewCount + 1 WHERE s.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
