package com._6.CourseManagerment.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class VideoStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("mp4", "webm", "mkv", "avi", "mov", "m4v");

    private static final long MAX_FILE_BYTES = 2L * 1024 * 1024 * 1024; // 2 GB

    @Value("${app.video.upload-dir:./uploads/videos}")
    private String uploadDirPath;

    private Path uploadDir;

    @PostConstruct
    public void init() throws Exception {
        Path configuredPath = Paths.get(uploadDirPath);
        if (configuredPath.isAbsolute()) {
            uploadDir = configuredPath.normalize();
        } else {
            // Resolve relative to the project root (CourseManagerment/), not the JVM working
            // directory which varies based on how the app is launched (VS Code vs mvnw).
            // getResource("/") returns the classpath root (target/classes/), so going up
            // two levels gives us the project root reliably.
            URL classesUrl = getClass().getResource("/");
            if (classesUrl != null) {
                Path projectRoot = Paths.get(classesUrl.toURI()).getParent().getParent();
                uploadDir = projectRoot.resolve(uploadDirPath).normalize();
            } else {
                uploadDir = configuredPath.toAbsolutePath().normalize();
            }
        }
        Files.createDirectories(uploadDir);
    }

    /**
     * Stores an uploaded video file and returns a "local:" URI string.
     *
     * @param file the uploaded multipart file
     * @return e.g. "local:uploads/videos/550e8400-e29b-41d4-a716-446655440000.mp4"
     */
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file must not be empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Video file exceeds maximum allowed size (2 GB)");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported video format: " + ext
                    + ". Allowed: " + ALLOWED_EXTENSIONS);
        }

        String uniqueName = UUID.randomUUID() + "." + ext;
        Path target = uploadDir.resolve(uniqueName).normalize();

        // Prevent path traversal – target must remain inside uploadDir
        if (!target.startsWith(uploadDir)) {
            throw new SecurityException("Invalid file path detected");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Return a portable relative reference prefixed with "local:"
        return "local:uploads/videos/" + uniqueName;
    }

    /**
     * Resolves a "local:…" URI to an absolute Path on disk.
     */
    public Path resolveLocalFile(String localUrl) {
        if (localUrl == null || !localUrl.startsWith("local:")) {
            throw new IllegalArgumentException("Not a local video URL: " + localUrl);
        }
        String relativePart = localUrl.substring("local:".length());
        // Extract only the filename — the stored path is always "uploads/videos/{filename}"
        // Using just the filename prevents path traversal and the double-path bug
        // that would occur if we naively resolved relativePart against uploadDir.getParent().
        String filename = Paths.get(relativePart).getFileName().toString();
        Path resolved = uploadDir.resolve(filename).normalize();

        // Safety: resolved path must remain inside uploadDir
        if (!resolved.startsWith(uploadDir)) {
            throw new SecurityException("Path traversal attempt detected");
        }
        return resolved;
    }

    /**
     * Deletes the stored video file associated with a "local:…" URL.
     * Silently does nothing if the URL is null, not local, or the file doesn't exist.
     */
    public void deleteByLocalUrl(String localUrl) {
        if (localUrl == null || !localUrl.startsWith("local:")) return;
        try {
            Path file = resolveLocalFile(localUrl);
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
            // Non-critical cleanup — log rather than propagate
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
