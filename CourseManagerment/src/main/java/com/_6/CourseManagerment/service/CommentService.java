package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.CommentDto;
import com._6.CourseManagerment.dto.CreateCommentRequest;
import com._6.CourseManagerment.dto.ReplyCommentRequest;
import com._6.CourseManagerment.entity.Comment;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.CommentRepository;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import com._6.CourseManagerment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    public List<CommentDto> getLessonComments(Long lessonId, Long currentUserId) {
        validatePurchasedAccess(lessonId, currentUserId);

        List<Comment> comments = commentRepository.findByLessonIdOrderByCreatedAtAsc(lessonId);
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, String> usernames = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        Map<Long, CommentDto> dtoMap = new HashMap<>();
        List<CommentDto> roots = new ArrayList<>();

        for (Comment comment : comments) {
            String username = usernames.getOrDefault(comment.getUserId(), "User " + comment.getUserId());
            CommentDto dto = new CommentDto(comment, username, currentUserId);
            dtoMap.put(comment.getId(), dto);
        }

        for (Comment comment : comments) {
            CommentDto dto = dtoMap.get(comment.getId());
            if (comment.getParentId() == null) {
                roots.add(dto);
            } else {
                CommentDto parent = dtoMap.get(comment.getParentId());
                if (parent != null) {
                    parent.getReplies().add(dto);
                } else {
                    roots.add(dto);
                }
            }
        }

        roots.sort(Comparator.comparing(CommentDto::getCreatedAt).reversed());
        return roots;
    }

    public CommentDto createComment(CreateCommentRequest request, Long currentUserId) {
        validatePurchasedAccess(request.getLessonId(), currentUserId);

        Comment comment = new Comment();
        comment.setUserId(currentUserId);
        comment.setLessonId(request.getLessonId());
        comment.setContent(request.getContent().trim());

        Comment saved = commentRepository.save(comment);
        String username = userRepository.findById(currentUserId).map(User::getUsername).orElse("User " + currentUserId);
        return new CommentDto(saved, username, currentUserId);
    }

    public CommentDto replyToComment(Long commentId, ReplyCommentRequest request, Long currentUserId) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        validatePurchasedAccess(parent.getLessonId(), currentUserId);

        Comment reply = new Comment();
        reply.setUserId(currentUserId);
        reply.setLessonId(parent.getLessonId());
        reply.setParentId(parent.getId());
        reply.setContent(request.getContent().trim());

        Comment saved = commentRepository.save(reply);
        String username = userRepository.findById(currentUserId).map(User::getUsername).orElse("User " + currentUserId);
        return new CommentDto(saved, username, currentUserId);
    }

    public CommentDto toggleLike(Long commentId, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        validatePurchasedAccess(comment.getLessonId(), currentUserId);

        Set<Long> liked = comment.getLikedUserIds();
        if (liked.contains(currentUserId)) {
            liked.remove(currentUserId);
        } else {
            liked.add(currentUserId);
        }

        Comment updated = commentRepository.save(comment);
        String username = userRepository.findById(updated.getUserId()).map(User::getUsername).orElse("User " + updated.getUserId());
        return new CommentDto(updated, username, currentUserId);
    }

    public void deleteComment(Long commentId, Long currentUserId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!isAdmin && !comment.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }

        deleteRecursive(comment);
    }

    private void deleteRecursive(Comment comment) {
        List<Comment> children = commentRepository.findByParentId(comment.getId());
        for (Comment child : children) {
            deleteRecursive(child);
        }
        commentRepository.delete(comment);
    }

    private void validatePurchasedAccess(Long lessonId, Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        boolean hasPurchased = enrollmentRepository.existsPurchasedAccess(currentUserId, lessonId);
        if (!hasPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must purchase this lesson to interact");
        }
    }
}
