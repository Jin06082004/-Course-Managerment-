package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCourseId(Long courseId);

    List<Exam> findByCourseAndInstructor(Long courseId, Long instructorId);

    List<Exam> findByInstructorId(Long instructorId);

    @Query("SELECT e FROM Exam e WHERE e.instructor.id = :instructorId AND e.isDraft = true")
    List<Exam> findDraftsByInstructorId(@Param("instructorId") Long instructorId);

    @Query("SELECT e FROM Exam e WHERE e.course.id = :courseId AND e.isPublished = true")
    List<Exam> findPublishedByCourseId(@Param("courseId") Long courseId);

    Optional<Exam> findByIdAndInstructorId(Long id, Long instructorId);
}
