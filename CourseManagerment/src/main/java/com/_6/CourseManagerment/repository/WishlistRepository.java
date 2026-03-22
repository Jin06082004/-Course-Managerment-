package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Wishlist Repository - Handles database operations for Wishlist entity
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    /**
     * Find all wishlist items for a user
     */
    Page<Wishlist> findByUser_Id(Long userId, Pageable pageable);

    /**
     * Check if a course is in user's wishlist
     */
    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);

    /**
     * Find wishlist item by user and course
     */
    Optional<Wishlist> findByUser_IdAndCourse_Id(Long userId, Long courseId);

    /**
     * Delete wishlist item by user and course
     */
    void deleteByUser_IdAndCourse_Id(Long userId, Long courseId);

    /**
     * Count wishlist items for a user
     */
    long countByUser_Id(Long userId);
}
