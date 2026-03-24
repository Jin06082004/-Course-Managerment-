package com._6.CourseManagerment.repository;

import java.util.List;
import com._6.CourseManagerment.entity.Role;
import com._6.CourseManagerment.entity.User;
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

    /**
     * Native query: search users with LEFT JOIN to roles (safe even when role_id is invalid)
     * Returns: id, username, email, fullName, avatar, status, createdAt, updatedAt, roleName, roleId
     */
    @Query(value = """
        SELECT u.id, u.username, u.email, u.full_name, u.avatar, u.status,
               u.created_at, u.updated_at, r.name AS role_name, u.role_id
        FROM users u
        LEFT JOIN roles r ON u.role_id = r.id
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY u.id DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM users u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """,
        nativeQuery = true)
    Page<Object[]> searchUsersNative(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Get all users with LEFT JOIN to roles (safe even when role_id is invalid)
     */
    @Query(value = """
        SELECT u.id, u.username, u.email, u.full_name, u.avatar, u.status,
               u.created_at, u.updated_at, r.name AS role_name, u.role_id
        FROM users u
        LEFT JOIN roles r ON u.role_id = r.id
        ORDER BY u.id DESC
        """,
        countQuery = "SELECT COUNT(*) FROM users u",
        nativeQuery = true)
    Page<Object[]> getAllUsersNative(Pageable pageable);

    /**
     * Get users by role_id with LEFT JOIN (safe even when role_id is invalid)
     */
    @Query(value = """
        SELECT u.id, u.username, u.email, u.full_name, u.avatar, u.status,
               u.created_at, u.updated_at, r.name AS role_name, u.role_id
        FROM users u
        LEFT JOIN roles r ON u.role_id = r.id
        WHERE u.role_id = :roleId
        ORDER BY u.id DESC
        """,
        countQuery = "SELECT COUNT(*) FROM users u WHERE u.role_id = :roleId",
        nativeQuery = true)
    Page<Object[]> getUsersByRoleNative(@Param("roleId") Long roleId, Pageable pageable);

    /**
     * Native query: count users by role_id
     */
    @Query(value = "SELECT COUNT(*) FROM users WHERE role_id = :roleId", nativeQuery = true)
    long countByRoleIdNative(@Param("roleId") Long roleId);

    /**
     * Get single user by ID using native query (safe even when role_id is invalid)
     */
    @Query(value = """
        SELECT u.id, u.username, u.email, u.full_name, u.avatar, u.status,
               u.created_at, u.updated_at, r.name AS role_name, u.role_id
        FROM users u
        LEFT JOIN roles r ON u.role_id = r.id
        WHERE u.id = :userId
        """,
        nativeQuery = true)
    List<Object[]> findUserByIdNative(@Param("userId") Long userId);

    /**
     * Update user's role_id directly via native SQL.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE users SET role_id = :roleId WHERE id = :userId", nativeQuery = true)
    void updateUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
