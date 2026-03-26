package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByExamIdOrderByQuestionOrder(Long examId);

    Long countByExamId(Long examId);

    void deleteByExamId(Long examId);
}
