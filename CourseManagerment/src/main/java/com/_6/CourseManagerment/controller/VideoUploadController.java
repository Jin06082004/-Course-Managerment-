package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.UserRepository;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.VideoStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
@PreAuthorize("hasRole('INSTRUCTOR')")
@Slf4j
public class VideoUploadController {

    @Autowired
    private VideoStorageService videoStorageService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Upload a video file for a course intro.
     * The stored "local:…" URL is saved to course.videoUrl.
     */
    @PostMapping("/courses/{courseId}/upload-video")
    @Transactional
    public ResponseEntity<?> uploadCourseVideo(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User instructor = userRepository.findById(userId).orElse(null);
            if (instructor == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            // Fetch course with instructor eagerly to avoid LazyInitializationException
            Course course = courseRepository.findByIdWithInstructor(courseId).orElse(null);
            if (course == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Course not found"));
            }

            // Verify ownership
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "You do not own this course"));
            }

            // Delete previous local video if replacing
            String previousUrl = course.getVideoUrl();
            if (previousUrl != null && previousUrl.startsWith("local:")) {
                videoStorageService.deleteByLocalUrl(previousUrl);
            }

            // Store new video and update course record
            String localUrl = videoStorageService.store(file);
            course.setVideoUrl(localUrl);
            courseRepository.save(course);

            log.info("Instructor {} uploaded video for course {}: {}", userId, courseId, localUrl);
            return ResponseEntity.ok(Map.of("videoUrl", localUrl, "message", "Video uploaded successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            log.warn("Security violation during video upload by user", e);
            return ResponseEntity.status(400).body(Map.of("error", "Invalid file path"));
        } catch (Exception e) {
            log.error("Error uploading video for course {}", courseId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
