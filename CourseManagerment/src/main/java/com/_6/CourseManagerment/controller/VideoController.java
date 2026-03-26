package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.VideoSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoSecurityService videoSecurityService;

    @GetMapping("/{id}/secure-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSecureVideoUrl(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> payload = videoSecurityService.generateSecureUrl(id, userId);

        String securePath = (String) payload.get("securePath");
        String absoluteUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path(securePath).toUriString();
        payload.put("secureUrl", absoluteUrl);
        payload.remove("securePath");

        return ResponseEntity.ok(payload);
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<?> streamVideo(
            @PathVariable Long id,
            @RequestParam("uid") Long userId,
            @RequestParam("exp") long exp,
            @RequestParam("sig") String sig,
            HttpServletRequest request) {

        String sourceUrl = videoSecurityService.validateAndResolveVideoUrl(id, userId, exp, sig);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(sourceUrl));
        headers.add("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        headers.add("Pragma", "no-cache");
        return ResponseEntity.status(302).headers(headers).build();
    }
}
