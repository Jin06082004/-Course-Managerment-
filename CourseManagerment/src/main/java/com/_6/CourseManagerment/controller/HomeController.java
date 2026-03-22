package com._6.CourseManagerment.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home Controller
 * Handles public pages using Thymeleaf templates
 */
@Controller
@Slf4j
public class HomeController {
    
    /**
     * Add authentication status to model for all requests
     */
    private void addAuthenticationToModel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");
        model.addAttribute("isAuthenticated", isAuthenticated);
    }
    
    /**
     * Home page
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Home");
        addAuthenticationToModel(model);
        return "home";
    }
    
    /**
     * Home page (alternative route)
     */
    @GetMapping("/home")
    public String homeAlternative() {
        return "redirect:/";
    }
}
