package com._6.CourseManagerment.service;

import com._6.CourseManagerment.config.JwtProperties;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoSecurityService {

    private static final long DEFAULT_EXPIRY_SECONDS = 3600; // 1 hour

    // Matches youtube.com/watch?v=ID, youtu.be/ID, youtube.com/embed/ID, youtube.com/shorts/ID
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
        "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})"
    );

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    public Map<String, Object> generateSecureUrl(Long lessonId, Long userId) {
        validatePurchasedAccess(lessonId, userId);

        Course course = courseRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        if (course.getVideoUrl() == null || course.getVideoUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This lesson has no video URL");
        }

        String rawUrl = course.getVideoUrl();
        Map<String, Object> payload = new HashMap<>();
        payload.put("videoId", lessonId);

        // YouTube: no stream redirect needed – return embed URL directly after auth check
        String youtubeId = extractYoutubeVideoId(rawUrl);
        if (youtubeId != null) {
            payload.put("videoType", "youtube");
            payload.put("embedUrl", "https://www.youtube.com/embed/" + youtubeId + "?rel=0&modestbranding=1");
            // Also provide a dummy secureUrl so existing JS null-check still passes
            long exp = Instant.now().getEpochSecond() + DEFAULT_EXPIRY_SECONDS;
            String signature = sign(lessonId, userId, exp);
            payload.put("secureUrl", String.format("/api/videos/%d/stream?uid=%d&exp=%d&sig=%s", lessonId, userId, exp, signature));
            return payload;
        }

        long exp = Instant.now().getEpochSecond() + DEFAULT_EXPIRY_SECONDS;
        String signature = sign(lessonId, userId, exp);

        payload.put("expiresAt", exp);
        payload.put("securePath", String.format("/api/videos/%d/stream?uid=%d&exp=%d&sig=%s", lessonId, userId, exp, signature));
        payload.put("secureUrl",  String.format("/api/videos/%d/stream?uid=%d&exp=%d&sig=%s", lessonId, userId, exp, signature));
        payload.put("streamType", rawUrl.contains(".m3u8") ? "hls" : "mp4");
        payload.put("videoType",  rawUrl.startsWith("local:") ? "local" : "external");
        return payload;
    }

    /**
     * Simplified access check + URL resolver — no HMAC signing.
     * Used by the /play endpoint which authenticates via JWT in ?t= param.
     */
    @Transactional(readOnly = true)
    public String resolveVideoUrlForUser(Long courseId, Long userId) {
        validatePurchasedAccess(courseId, userId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
        String url = course.getVideoUrl();
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This lesson has no video URL");
        }
        return url;
    }

    /** Returns the 11-char YouTube video ID, or null if not a YouTube URL. */
    private String extractYoutubeVideoId(String url) {
        if (url == null) return null;
        Matcher m = YOUTUBE_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    @Transactional(readOnly = true)
    public String validateAndResolveVideoUrl(Long lessonId, Long userId, long exp, String sig) {
        long now = Instant.now().getEpochSecond();
        if (exp < now) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Video link has expired");
        }

        String expected = sign(lessonId, userId, exp);
        if (!expected.equals(sig)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid video signature");
        }

        validatePurchasedAccess(lessonId, userId);

        Course course = courseRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        String sourceUrl = course.getVideoUrl();
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This lesson has no video URL");
        }

        return sourceUrl;
    }

    private void validatePurchasedAccess(Long lessonId, Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        // Instructor who owns the course can always access their own video
        // Use JOIN FETCH to avoid LazyInitializationException on course.getInstructor()
        Course course = courseRepository.findByIdWithInstructor(lessonId).orElse(null);
        if (course != null && course.getInstructor() != null
                && course.getInstructor().getId().equals(userId)) {
            return;
        }

        boolean hasPurchased = enrollmentRepository.existsPurchasedAccess(userId, lessonId);
        if (!hasPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must purchase this course to access video");
        }
    }

    private String sign(Long lessonId, Long userId, long exp) {
        try {
            String data = lessonId + ":" + userId + ":" + exp;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot sign video URL");
        }
    }
}
