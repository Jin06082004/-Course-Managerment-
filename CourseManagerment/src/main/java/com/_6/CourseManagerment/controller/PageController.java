package com._6.CourseManagerment.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    
    /**
     * Add authentication status to model for all requests
     */
    private void addAuthenticationToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");
        model.addAttribute("isAuthenticated", isAuthenticated);
    }
    
    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("title", "Courses");
        addAuthenticationToModel(model);
        return "courses";
    }
    
    @GetMapping("/courses/{id}")
    public String courseDetail(Model model) {
        model.addAttribute("title", "Course Detail");
        addAuthenticationToModel(model);
        return "course-detail";
    }
    
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("title", "Login");
        addAuthenticationToModel(model);
        return "login";
    }
    
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("title", "Register");
        addAuthenticationToModel(model);
        return "register";
    }
    
    @GetMapping("/my-courses")
    public String myCourses(Model model) {
        model.addAttribute("title", "My Courses");
        addAuthenticationToModel(model);
        return "my-courses";
    }
}
