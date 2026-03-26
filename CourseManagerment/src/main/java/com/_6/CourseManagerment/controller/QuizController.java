package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.dto.LessonQuizDto;
import com._6.CourseManagerment.dto.QuizSubmitRequest;
import com._6.CourseManagerment.dto.QuizSubmitResultDto;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.QuizService;
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
@RequestMapping("/api")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @GetMapping("/lessons/{id}/quiz")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonQuizDto> getLessonQuiz(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(quizService.getLessonQuiz(id, userId));
    }

    @PostMapping("/quiz/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuizSubmitResultDto> submitQuiz(@Valid @RequestBody QuizSubmitRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(quizService.submitQuiz(request, userId));
    }
}
