package com._6.CourseManagerment.service;

import com._6.CourseManagerment.config.JwtProperties;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class VideoSecurityService {

    private static final long DEFAULT_EXPIRY_SECONDS = 300;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private JwtProperties jwtProperties;

    public Map<String, Object> generateSecureUrl(Long lessonId, Long userId) {
        validatePurchasedAccess(lessonId, userId);

        Course course = courseRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        if (course.getVideoUrl() == null || course.getVideoUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This lesson has no video URL");
        }

        long exp = Instant.now().getEpochSecond() + DEFAULT_EXPIRY_SECONDS;
        String signature = sign(lessonId, userId, exp);

        Map<String, Object> payload = new HashMap<>();
        payload.put("videoId", lessonId);
        payload.put("expiresAt", exp);
        payload.put("securePath", String.format("/api/videos/%d/stream?uid=%d&exp=%d&sig=%s", lessonId, userId, exp, signature));
        payload.put("streamType", course.getVideoUrl().contains(".m3u8") ? "hls" : "mp4");
        return payload;
    }

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
