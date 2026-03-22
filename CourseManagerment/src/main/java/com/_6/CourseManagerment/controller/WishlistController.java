package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.WishlistDto;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.WishlistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Wishlist Controller
 * Endpoints:
 * - GET    /api/wishlist          - Get user's wishlist
 * - POST   /api/wishlist/toggle/{courseId} - Toggle wishlist item
 * - DELETE /api/wishlist/{courseId}        - Remove from wishlist
 * - GET    /api/wishlist/check/{courseId}  - Check if course in wishlist
 */
@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("isAuthenticated()")
@Slf4j
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    /**
     * Get current user's wishlist
     */
    @GetMapping
    public ResponseEntity<?> getWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<WishlistDto> wishlist = wishlistService.getUserWishlist(userId, pageable);
            return ResponseEntity.ok(wishlist);
        } catch (Exception e) {
            log.error("Failed to get wishlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Toggle course in wishlist (add if not exists, remove if exists)
     */
    @PostMapping("/toggle/{courseId}")
    public ResponseEntity<?> toggleWishlist(@PathVariable Long courseId) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            boolean added = wishlistService.toggleWishlist(userId, courseId);
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("courseId", courseId);
                put("inWishlist", added);
                put("message", added ? "Added to wishlist" : "Removed from wishlist");
            }});
        } catch (Exception e) {
            log.error("Failed to toggle wishlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Add course to wishlist
     */
    @PostMapping("/{courseId}")
    public ResponseEntity<?> addToWishlist(@PathVariable Long courseId) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            WishlistDto item = wishlistService.addToWishlist(userId, courseId);
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (Exception e) {
            log.error("Failed to add to wishlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Remove course from wishlist
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long courseId) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            wishlistService.removeFromWishlist(userId, courseId);
            return ResponseEntity.ok(new HashMap<String, String>() {{ put("message", "Removed from wishlist"); }});
        } catch (Exception e) {
            log.error("Failed to remove from wishlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }

    /**
     * Check if course is in wishlist
     */
    @GetMapping("/check/{courseId}")
    public ResponseEntity<?> checkWishlist(@PathVariable Long courseId) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            boolean inWishlist = wishlistService.isInWishlist(userId, courseId);
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("courseId", courseId);
                put("inWishlist", inWishlist);
            }});
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new HashMap<String, String>() {{ put("error", e.getMessage()); }});
        }
    }
}
