package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository - Handles database operations for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by email or username
     */
    Optional<User> findByEmailOrUsername(String email, String username);
    
    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users by role with pagination
     */
    Page<User> findByRole(Role role, Pageable pageable);
    
    /**
     * Count users by role
     */
    long countByRole(Role role);
    
    /**
     * Search users by username, email, or full name
     */
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username, 
            String email, 
            String fullName, 
            Pageable pageable);
}
