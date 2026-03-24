package com._6.CourseManagerment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Instructor Page Controller - Handles instructor page navigation.
 * HTML routes are permitAll in SecurityConfig; role checks for API calls use /api/instructor/**.
 */
@Controller
@RequestMapping("/instructor")
public class InstructorPageController {
    
    /**
     * Instructor dashboard
     */
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Instructor Dashboard");
        return "instructor/dashboard";
    }
    
    /**
     * My courses page
     */
    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("pageTitle", "My Courses");
        return "instructor/courses";
    }
    
    /**
     * Create new course page
     */
    @GetMapping("/courses/new")
    public String createCourse(Model model) {
        model.addAttribute("pageTitle", "Create New Course");
        return "instructor/create-course";
    }
    
    /**
     * Edit course page
     */
    @GetMapping("/courses/{id}/edit")
    public String editCourse(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Edit Course");
        model.addAttribute("courseId", id);
        return "instructor/edit-course";
    }
    
    /**
     * Course students/enrollments page
     */
    @GetMapping("/courses/{id}/students")
    public String courseStudents(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Course Students");
        model.addAttribute("courseId", id);
        return "instructor/course-students";
    }
    
    /**
     * Course content/materials page
     */
    @GetMapping("/courses/{id}/content")
    public String courseContent(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Course Content");
        model.addAttribute("courseId", id);
        return "instructor/course-content";
    }
    
    /**
     * Instructor statistics page
     */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        model.addAttribute("pageTitle", "My Statistics");
        return "instructor/statistics";
    }
    
    /**
     * Instructor earnings page
     */
    @GetMapping("/earnings")
    public String earnings(Model model) {
        model.addAttribute("pageTitle", "My Earnings");
        return "instructor/earnings";
    }
    
    /**
     * Instructor settings page
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("pageTitle", "Instructor Settings");
        return "instructor/settings";
    }
}
