package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.CommentDto;
import com._6.CourseManagerment.dto.CreateCommentRequest;
import com._6.CourseManagerment.dto.ReplyCommentRequest;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/lessons/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentDto>> getLessonComments(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(commentService.getLessonComments(id, userId));
    }

    @PostMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDto> createComment(@Valid @RequestBody CreateCommentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CommentDto created = commentService.createComment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/comments/{id}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDto> replyComment(@PathVariable Long id, @Valid @RequestBody ReplyCommentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CommentDto created = commentService.replyToComment(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/comments/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDto> likeComment(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(commentService.toggleLike(id, userId));
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Authentication authentication) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        commentService.deleteComment(id, userId, isAdmin);
        return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
    }
}
