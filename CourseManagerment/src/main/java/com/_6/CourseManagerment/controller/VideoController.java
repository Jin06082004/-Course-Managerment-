package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.VideoSecurityService;
import com._6.CourseManagerment.service.VideoStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@Slf4j
public class VideoController {

    @Autowired
    private VideoSecurityService videoSecurityService;

    @Autowired
    private VideoStorageService videoStorageService;

    @GetMapping("/{id}/secure-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSecureVideoUrl(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> payload = videoSecurityService.generateSecureUrl(id, userId);

        // Convert securePath (relative) to secureUrl if not already set.
        // Use simple concatenation — NOT UriComponentsBuilder.path() which encodes '?' to '%3F'.
        String securePath = (String) payload.get("securePath");
        if (securePath != null && !payload.containsKey("secureUrl")) {
            payload.put("secureUrl", securePath);
        }
        payload.remove("securePath");

        return ResponseEntity.ok(payload);
    }

    /**
     * Simple video play endpoint — authenticates via JWT in ?t= query param.
     * The HTML5 video element cannot send Bearer headers, so the JS passes
     * the token as:  <video src="/api/videos/1/play?t=JWT_TOKEN">
     * Spring Security authenticates the user via JwtAuthenticationFilter
     * (which now also reads the ?t= param), then this method checks enrollment.
     */
    @GetMapping("/{id}/play")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> playVideo(
            @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();
        try {
            String sourceUrl = videoSecurityService.resolveVideoUrlForUser(id, userId);

            if (sourceUrl.startsWith("local:")) {
                return streamLocalFile(sourceUrl, request);
            }
            // External URL: redirect
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(sourceUrl));
            return ResponseEntity.status(302).headers(headers).build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<?> streamVideo(
            @PathVariable Long id,
            @RequestParam("uid") Long userId,
            @RequestParam("exp") long exp,
            @RequestParam("sig") String sig,
            HttpServletRequest request) {

        String sourceUrl = videoSecurityService.validateAndResolveVideoUrl(id, userId, exp, sig);

        // Serve files uploaded to local storage with byte-range support
        if (sourceUrl != null && sourceUrl.startsWith("local:")) {
            return streamLocalFile(sourceUrl, request);
        }

        // Fall back to redirect for external URLs (e.g. CDN, S3 pre-signed)
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(sourceUrl));
        headers.add("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        headers.add("Pragma", "no-cache");
        return ResponseEntity.status(302).headers(headers).build();
    }

    // ── local file streaming ─────────────────────────────────────────────────

    private ResponseEntity<?> streamLocalFile(String localUrl, HttpServletRequest request) {
        try {
            Path filePath = videoStorageService.resolveLocalFile(localUrl);
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Video file not found on server");
            }

            long fileSize = Files.size(filePath);
            String contentType = detectContentType(filePath);

            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            if (rangeHeader == null || rangeHeader.isEmpty()) {
                // No Range header – stream whole file
                StreamingResponseBody body = out -> {
                    try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
                        byte[] buf = new byte[65536];
                        int read;
                        while ((read = raf.read(buf)) != -1) {
                            out.write(buf, 0, read);
                        }
                    }
                };
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, contentType);
                headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
                headers.setContentLength(fileSize);
                return ResponseEntity.ok().headers(headers).body(body);
            }

            // Parse "bytes=start-end"
            long[] range = parseRange(rangeHeader, fileSize);
            if (range == null) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }

            long start = range[0];
            long end   = range[1];
            long length = end - start + 1;

            StreamingResponseBody body = out -> {
                try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
                    raf.seek(start);
                    byte[] buf = new byte[65536];
                    long remaining = length;
                    int read;
                    while (remaining > 0 &&
                           (read = raf.read(buf, 0, (int) Math.min(buf.length, remaining))) != -1) {
                        out.write(buf, 0, read);
                        remaining -= read;
                    }
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            headers.setContentLength(length);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(headers).body(body);

        } catch (SecurityException e) {
            log.warn("Path traversal attempt in video stream", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        } catch (IOException e) {
            log.error("IO error streaming local video: {}", localUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Streaming error");
        }
    }

    private String detectContentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".mp4"))  return "video/mp4";
        if (name.endsWith(".webm")) return "video/webm";
        if (name.endsWith(".mkv"))  return "video/x-matroska";
        if (name.endsWith(".avi"))  return "video/x-msvideo";
        if (name.endsWith(".mov"))  return "video/quicktime";
        if (name.endsWith(".m4v"))  return "video/mp4";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /** Returns [start, end] or null if the Range header is unparseable / out of bounds. */
    private long[] parseRange(String rangeHeader, long fileSize) {
        try {
            if (!rangeHeader.startsWith("bytes=")) return null;
            String rangeValue = rangeHeader.substring("bytes=".length());
            String[] parts = rangeValue.split("-");
            long start = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0].trim());
            long end   = (parts.length < 2 || parts[1].isEmpty())
                    ? fileSize - 1
                    : Long.parseLong(parts[1].trim());
            if (start < 0 || end >= fileSize || start > end) return null;
            return new long[]{start, end};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
