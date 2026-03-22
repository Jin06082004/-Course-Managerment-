package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.WishlistDto;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.entity.Wishlist;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.UserRepository;
import com._6.CourseManagerment.repository.WishlistRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Add a course to user's wishlist
     */
    public WishlistDto addToWishlist(Long userId, Long courseId) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new Exception("Course not found"));

        if (wishlistRepository.existsByUser_IdAndCourse_Id(userId, courseId)) {
            throw new Exception("Course already in wishlist");
        }

        Wishlist wishlist = new Wishlist(user, course);
        Wishlist saved = wishlistRepository.save(wishlist);
        log.info("Course {} added to wishlist for user {}", courseId, userId);
        return new WishlistDto(saved);
    }

    /**
     * Remove a course from user's wishlist
     */
    public void removeFromWishlist(Long userId, Long courseId) throws Exception {
        Wishlist wishlist = wishlistRepository.findByUser_IdAndCourse_Id(userId, courseId)
            .orElseThrow(() -> new Exception("Wishlist item not found"));

        wishlistRepository.delete(wishlist);
        log.info("Course {} removed from wishlist for user {}", courseId, userId);
    }

    /**
     * Toggle wishlist: add if not exists, remove if exists
     */
    public boolean toggleWishlist(Long userId, Long courseId) throws Exception {
        if (wishlistRepository.existsByUser_IdAndCourse_Id(userId, courseId)) {
            removeFromWishlist(userId, courseId);
            return false;
        } else {
            addToWishlist(userId, courseId);
            return true;
        }
    }

    /**
     * Get all wishlist items for a user
     */
    public Page<WishlistDto> getUserWishlist(Long userId, Pageable pageable) {
        return wishlistRepository.findByUser_Id(userId, pageable)
            .map(WishlistDto::new);
    }

    /**
     * Check if a course is in user's wishlist
     */
    public boolean isInWishlist(Long userId, Long courseId) {
        return wishlistRepository.existsByUser_IdAndCourse_Id(userId, courseId);
    }
}
