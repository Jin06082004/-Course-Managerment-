package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    // Find by user and course
    Optional<Enrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);
    
    // Find all enrollments for a user
    Page<Enrollment> findByUser_Id(Long userId, Pageable pageable);
    
    // Find all enrollments for a course
    Page<Enrollment> findByCourse_Id(Long courseId, Pageable pageable);
    
    // Get enrollments by user and status
    Page<Enrollment> findByUser_IdAndStatus(Long userId, String status, Pageable pageable);
    
    // Get active enrollments
    @Query("SELECT e FROM Enrollment e WHERE e.user.id = :userId AND e.status IN ('ENROLLED', 'IN_PROGRESS') ORDER BY e.lastAccessedDate DESC")
    Page<Enrollment> findActiveEnrollments(@Param("userId") Long userId, Pageable pageable);
    
    // Get completed enrollments
    @Query("SELECT e FROM Enrollment e WHERE e.user.id = :userId AND e.status = 'COMPLETED' ORDER BY e.completionDate DESC")
    Page<Enrollment> findCompletedEnrollments(@Param("userId") Long userId, Pageable pageable);
    
    // Check if user enrolled in course
    Boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);
    
    // Count enrollments for a course
    Integer countByCourse_Id(Long courseId);
}
