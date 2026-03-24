package com._6.CourseManagerment.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin Page Controller - Handles admin page navigation
 * Role-based access control is handled client-side via JavaScript
 */
@Controller
@RequestMapping("/admin")
public class AdminPageController {
    
    /**
     * Admin dashboard
     */
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard");
        return "admin/dashboard";
    }
    
    /**
     * User management page
     */
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("pageTitle", "User Management");
        return "admin/users";
    }
    
    /**
     * Edit user page
     */
    @GetMapping("/users/{id}")
    public String editUser(Model model) {
        model.addAttribute("pageTitle", "Edit User");
        return "admin/edit-user";
    }
    
    /**
     * Role management page
     */
    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("pageTitle", "Role Management");
        return "admin/roles";
    }
    
    /**
     * Category management page
     */
    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("pageTitle", "Category Management");
        return "admin/categories";
    }
    
    /**
     * Course moderation page
     */
    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("pageTitle", "Course Moderation");
        return "admin/courses";
    }
    
    /**
     * Statistics/Reports page
     */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        model.addAttribute("pageTitle", "Statistics & Reports");
        return "admin/statistics";
    }
    
    /**
     * Settings page for admins
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Admin Settings");
        return "admin/settings";
    }
}
