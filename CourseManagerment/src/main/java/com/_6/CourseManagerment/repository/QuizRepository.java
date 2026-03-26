package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId ORDER BY q.createdAt DESC")
    List<Quiz> findByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId AND q.instructor.id = :instructorId ORDER BY q.createdAt DESC")
    List<Quiz> findByCourseAndInstructor(@Param("courseId") Long courseId, @Param("instructorId") Long instructorId);

    @Query("SELECT q FROM Quiz q WHERE q.lesson.id = :lessonId")
    Optional<Quiz> findByLessonId(@Param("lessonId") Long lessonId);

    @Query("SELECT q FROM Quiz q WHERE q.instructor.id = :instructorId ORDER BY q.createdAt DESC")
    List<Quiz> findByInstructorId(@Param("instructorId") Long instructorId);

    @Query("SELECT q FROM Quiz q WHERE q.isPublished = true AND q.course.id = :courseId ORDER BY q.createdAt DESC")
    List<Quiz> findPublishedByCourseid(@Param("courseId") Long courseId);

    @Query("SELECT q FROM Quiz q WHERE q.isDraft = true AND q.instructor.id = :instructorId ORDER BY q.updatedAt DESC")
    List<Quiz> findDraftsByInstructorId(@Param("instructorId") Long instructorId);
}
