package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    // Find by title containing
    Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    // Find by category
    Page<Course> findByCategory_Id(Long categoryId, Pageable pageable);
    
    // Find by instructor
    Page<Course> findByInstructor_Id(Long instructorId, Pageable pageable);
    
    // Find by instructor (User entity) with pagination
    Page<Course> findByInstructor(User instructor, Pageable pageable);
    
    // Find by instructor (User entity) without pagination
    List<Course> findByInstructor(User instructor);
    
    // Find by level
    Page<Course> findByLevel(String level, Pageable pageable);
    
    // Find by status
    Page<Course> findByStatus(String status, Pageable pageable);
    
    // Find by unique code
    Optional<Course> findByCode(String code);
    
    // Check if code exists
    Boolean existsByCode(String code);

    // Fetch course with its instructor eagerly (for ownership checks)
    @Query("SELECT c FROM Course c JOIN FETCH c.instructor WHERE c.id = :id")
    Optional<Course> findByIdWithInstructor(@Param("id") Long id);
    
    // Custom query for search with multiple criteria
    @Query("SELECT c FROM Course c WHERE " +
           "c.status = 'PUBLISHED' AND " +
           "(:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:categoryId IS NULL OR c.category.id = :categoryId) AND " +
           "(:level IS NULL OR c.level = :level)")
    Page<Course> searchCourses(@Param("title") String title,
                               @Param("categoryId") Long categoryId,
                               @Param("level") String level,
                               Pageable pageable);
    
    // Find featured courses (highest rated, published)
    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' ORDER BY c.rating DESC, c.studentCount DESC")
    List<Course> findFeaturedCourses(Pageable pageable);
    
    // Find top ratings
    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' ORDER BY c.rating DESC")
    Page<Course> findTopRated(Pageable pageable);

    // Count by status
    long countByStatus(String status);
}
