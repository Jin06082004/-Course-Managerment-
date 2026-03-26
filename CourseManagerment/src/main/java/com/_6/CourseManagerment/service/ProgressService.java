package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.ProgressRequest;
import com._6.CourseManagerment.dto.ProgressResponse;
import com._6.CourseManagerment.entity.LessonProgress;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import com._6.CourseManagerment.repository.LessonProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProgressService {

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    public ProgressResponse saveProgress(Long userId, ProgressRequest request) {
        validatePurchasedAccess(request.getLessonId(), userId);

        double safeTime = request.getCurrentTime() == null ? 0.0 : Math.max(0.0, request.getCurrentTime());

        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(userId, request.getLessonId())
                .orElseGet(() -> {
                    LessonProgress created = new LessonProgress();
                    created.setUserId(userId);
                    created.setLessonId(request.getLessonId());
                    created.setLastWatchedTime(0.0);
                    return created;
                });

        // Keep only the farthest watched point to prevent rollback abuse.
        if (safeTime > progress.getLastWatchedTime()) {
            progress.setLastWatchedTime(safeTime);
        }

        LessonProgress saved = lessonProgressRepository.save(progress);
        return new ProgressResponse(saved.getLessonId(), saved.getLastWatchedTime());
    }

    public ProgressResponse getProgress(Long userId, Long lessonId) {
        validatePurchasedAccess(lessonId, userId);

        LessonProgress progress = lessonProgressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElse(null);

        if (progress == null) {
            return new ProgressResponse(lessonId, 0.0);
        }

        return new ProgressResponse(progress.getLessonId(), progress.getLastWatchedTime());
    }

    private void validatePurchasedAccess(Long lessonId, Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        boolean hasPurchased = enrollmentRepository.existsPurchasedAccess(userId, lessonId);
        if (!hasPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must purchase this course to access progress");
        }
    }
}
