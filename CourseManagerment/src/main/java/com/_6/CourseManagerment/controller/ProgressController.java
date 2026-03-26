package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.ProgressRequest;
import com._6.CourseManagerment.dto.ProgressResponse;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.ProgressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    @Autowired
    private ProgressService progressService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProgressResponse> saveProgress(@Valid @RequestBody ProgressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(progressService.saveProgress(userId, request));
    }

    @GetMapping("/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProgressResponse> getProgress(@PathVariable Long lessonId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(progressService.getProgress(userId, lessonId));
    }
}
