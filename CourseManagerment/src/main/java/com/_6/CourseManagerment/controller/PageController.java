package com._6.CourseManagerment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    @GetMapping("/courses")
    public String courses() {
        return "courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail() {
        return "course-detail";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/my-courses")
    public String myCourses() {
        return "my-courses";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/wishlist")
    public String wishlist() {
        return "wishlist";
    }

    @GetMapping("/error/404")
    public String error404(Model model) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Trang không tìm thấy");
        model.addAttribute("errorMessage", "Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển. Vui lòng kiểm tra lại URL.");
        return "error";
    }

    @GetMapping("/error/500")
    public String error500(Model model) {
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "Lỗi máy chủ nội bộ");
        model.addAttribute("errorMessage", "Đã xảy ra lỗi phía máy chủ. Vui lòng thử lại sau.");
        return "error";
    }

    @GetMapping("/error")
    public String error(@RequestParam(required = false, defaultValue = "500") int code, Model model) {
        model.addAttribute("errorCode", String.valueOf(code));
        if (code == 403) {
            model.addAttribute("errorTitle", "Không có quyền truy cập");
            model.addAttribute("errorMessage", "Bạn không có quyền truy cập trang này.");
        } else if (code == 401) {
            model.addAttribute("errorTitle", "Yêu cầu đăng nhập");
            model.addAttribute("errorMessage", "Vui lòng đăng nhập để truy cập trang này.");
        } else {
            model.addAttribute("errorTitle", "Đã xảy ra lỗi");
            model.addAttribute("errorMessage", "Đã xảy ra lỗi không xác định. Vui lòng thử lại sau.");
        }
        return "error";
    }
}
