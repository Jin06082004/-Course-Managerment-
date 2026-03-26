package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    Optional<ExamResult> findByUserIdAndExamId(Long userId, Long examId);

    List<ExamResult> findByExamId(Long examId);

    List<ExamResult> findByUserId(Long userId);

    @Query("SELECT er FROM ExamResult er WHERE er.exam.id = :examId ORDER BY er.submittedAt DESC")
    List<ExamResult> findByExamIdOrderBySubmittedAtDesc(@Param("examId") Long examId);

    @Query("SELECT COUNT(er) FROM ExamResult er WHERE er.exam.id = :examId AND er.passed = true")
    Long countPassedByExamId(@Param("examId") Long examId);
}
