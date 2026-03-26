package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    @Query("SELECT q FROM QuizQuestion q WHERE q.quiz.id = :quizId ORDER BY q.questionOrder ASC")
    List<QuizQuestion> findByQuizIdOrderByOrder(@Param("quizId") Long quizId);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    Integer countByQuizId(@Param("quizId") Long quizId);

    @Query("DELETE FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);
}
