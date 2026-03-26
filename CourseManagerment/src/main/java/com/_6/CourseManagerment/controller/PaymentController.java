package com._6.CourseManagerment.controller;

import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.security.SecurityUtils;
import com._6.CourseManagerment.service.EnrollmentService;
import com._6.CourseManagerment.service.MoMoPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller xử lý các API thanh toán qua MoMo Payment Gateway.
 * Kết nối thanh toán với enrollment: user phải trả tiền khóa học trước khi được học.
 */
@RestController
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {

    @Autowired
    private MoMoPaymentService moMoPaymentService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * Tạo đơn thanh toán MoMo cho khóa học.
     * 1. Lấy thông tin khóa học và giá
     * 2. Tạo enrollment trạng thái PENDING_PAYMENT
     * 3. Gọi MoMo API để tạo đơn thanh toán
     * 4. Trả payUrl cho frontend redirect người dùng sang MoMo
     *
     * @param request chứa courseId
     * @return payUrl để redirect người dùng
     */
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, String> request) {
        try {
            String courseIdStr = request.get("courseId");
            if (courseIdStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Thiếu tham số: courseId là bắt buộc"));
            }

            Long courseId = Long.parseLong(courseIdStr);
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User chưa đăng nhập"));
            }

            // Lấy thông tin khóa học
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));

            // Kiểm tra khóa học có phí không
            if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Khóa học này miễn phí, không cần thanh toán"));
            }

            // Tạo orderId duy nhất: COURSE_{courseId}_{userId}_{uuid}
            String orderId = "COURSE_" + courseId + "_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8);
            long amount = course.getPrice().longValue(); // MoMo yêu cầu số nguyên (VND)
            String orderInfo = "Thanh toan khoa hoc: " + course.getTitle();

            // Tạo enrollment trạng thái chờ thanh toán (hoặc cập nhật orderId nếu đã có PENDING)
            enrollmentService.createPendingEnrollment(userId, courseId, orderId);

            // Gọi MoMo tạo đơn thanh toán
            String payUrl = moMoPaymentService.createPayment(orderId, amount, orderInfo);

            Map<String, Object> response = new HashMap<>();
            response.put("payUrl", payUrl);
            response.put("orderId", orderId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Lỗi tạo thanh toán MoMo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Không thể tạo đơn thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Endpoint nhận redirect từ MoMo sau khi người dùng thanh toán xong.
     * MoMo redirect về URL này kèm kết quả thanh toán.
     * Nếu thành công → kích hoạt enrollment và redirect về trang khóa học.
     * Nếu thất bại → redirect về trang khóa học với thông báo lỗi.
     */
    @GetMapping("/momo-return")
    public ResponseEntity<?> momoReturn(@RequestParam Map<String, String> params) {
        log.info("MoMo return callback: orderId={}, resultCode={}",
                params.get("orderId"), params.get("resultCode"));

        String resultCode = params.getOrDefault("resultCode", "-1");
        String orderId = params.getOrDefault("orderId", "");

        if ("0".equals(resultCode)) {
            // Thanh toán thành công → kích hoạt enrollment
            try {
                enrollmentService.activateEnrollmentByOrderId(orderId);
                log.info("Payment success, enrollment activated: orderId={}", orderId);
            } catch (Exception e) {
                log.error("Lỗi kích hoạt enrollment từ momo-return: {}", e.getMessage());
            }
        }

        // Trích courseId từ orderId (format: COURSE_{courseId}_{userId}_{uuid})
        String courseId = "1";
        try {
            String[] parts = orderId.split("_");
            if (parts.length >= 2) {
                courseId = parts[1];
            }
        } catch (Exception ignored) {}

        // Redirect về trang course detail với kết quả
        String redirectUrl = "/courses/" + courseId + "?payment=" + ("0".equals(resultCode) ? "success" : "failed");
        return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
    }

    /**
     * Endpoint nhận IPN (Instant Payment Notification) / Webhook từ MoMo.
     * MoMo gọi POST đến URL này để thông báo kết quả thanh toán server-to-server.
     * Bắt buộc xác thực chữ ký trước khi cập nhật trạng thái enrollment.
     */
    @PostMapping("/momo-notify")
    public ResponseEntity<Void> momoNotify(@RequestBody Map<String, String> params) {
        log.info("MoMo IPN callback: orderId={}, resultCode={}",
                params.get("orderId"), params.get("resultCode"));

        // Xác thực chữ ký từ MoMo để chống giả mạo
        boolean isValid = moMoPaymentService.verifyIpnSignature(params);
        if (!isValid) {
            log.warn("MoMo IPN signature không hợp lệ! Bỏ qua request.");
            return ResponseEntity.noContent().build();
        }

        String resultCode = params.getOrDefault("resultCode", "-1");
        String orderId = params.getOrDefault("orderId", "");

        if ("0".equals(resultCode)) {
            // Thanh toán thành công → kích hoạt enrollment
            try {
                enrollmentService.activateEnrollmentByOrderId(orderId);
                log.info("Enrollment activated: orderId={}, transId={}", orderId, params.get("transId"));
            } catch (Exception e) {
                log.error("Lỗi kích hoạt enrollment sau thanh toán: orderId={}, error={}", orderId, e.getMessage());
            }
        } else {
            log.warn("Thanh toán thất bại: orderId={}, resultCode={}, message={}",
                    orderId, resultCode, params.get("message"));
        }

        // MoMo yêu cầu trả về HTTP 204 No Content
        return ResponseEntity.noContent().build();
    }
}
