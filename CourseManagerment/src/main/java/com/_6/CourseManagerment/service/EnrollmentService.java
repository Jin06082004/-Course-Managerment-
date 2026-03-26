package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.EnrollmentDto;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.Enrollment;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import com._6.CourseManagerment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class EnrollmentService {
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    /**
     * Enroll user in a course.
     * - Khóa học miễn phí (price == null hoặc <= 0): tạo enrollment ngay, status = ENROLLED, paymentStatus = FREE
     * - Khóa học có phí (price > 0): tạo enrollment với status = PENDING_PAYMENT, paymentStatus = PENDING
     *   → User phải thanh toán qua MoMo trước khi được học
     */
    public EnrollmentDto enrollUserInCourse(Long userId, Long courseId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new Exception("Course not found"));
        
        // Check if already enrolled
        if (enrollmentRepository.existsByUser_IdAndCourse_Id(userId, courseId)) {
            throw new Exception("User already enrolled in this course");
        }
        
        Enrollment enrollment = new Enrollment(user, course);
        
        boolean isFree = course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0;
        
        if (isFree) {
            // Khóa học miễn phí → cho học ngay
            enrollment.setStatus("ENROLLED");
            enrollment.setPaymentStatus("FREE");
        } else {
            // Khóa học có phí → chờ thanh toán
            enrollment.setStatus("PENDING_PAYMENT");
            enrollment.setPaymentStatus("PENDING");
        }
        
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        
        // Chỉ tăng studentCount khi enrollment thực sự active (miễn phí)
        if (isFree) {
            course.setStudentCount(enrollmentRepository.countByCourse_Id(courseId));
            courseRepository.save(course);
        }
        
        log.info("User {} enrolled in course {} (paymentStatus={})", userId, courseId, enrollment.getPaymentStatus());
        return new EnrollmentDto(savedEnrollment);
    }
    
    /**
     * Kích hoạt enrollment sau khi thanh toán MoMo thành công.
     * Được gọi từ MoMo IPN callback.
     *
     * @param orderId mã đơn hàng liên kết với enrollment
     */
    public void activateEnrollmentByOrderId(String orderId) throws Exception {
        Enrollment enrollment = enrollmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new Exception("Enrollment not found for orderId: " + orderId));
        
        if ("PAID".equals(enrollment.getPaymentStatus())) {
            log.warn("Enrollment đã được kích hoạt trước đó: orderId={}", orderId);
            return;
        }
        
        enrollment.setPaymentStatus("PAID");
        enrollment.setStatus("ENROLLED");
        enrollmentRepository.save(enrollment);
        
        // Cập nhật studentCount cho course
        Course course = enrollment.getCourse();
        course.setStudentCount(enrollmentRepository.countByCourse_Id(course.getId()));
        courseRepository.save(course);
        
        log.info("Enrollment activated after payment: orderId={}, userId={}, courseId={}",
                orderId, enrollment.getUser().getId(), course.getId());
    }
    
    /**
     * Tạo enrollment cho khóa học có phí và gắn orderId để liên kết với MoMo payment.
     * Được gọi khi user bấm thanh toán (trước khi redirect sang MoMo).
     *
     * @return EnrollmentDto chứa thông tin enrollment đã tạo
     */
    public EnrollmentDto createPendingEnrollment(Long userId, Long courseId, String orderId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new Exception("Course not found"));
        
        // Kiểm tra đã enrolled chưa
        var existingOpt = enrollmentRepository.findByUser_IdAndCourse_Id(userId, courseId);
        if (existingOpt.isPresent()) {
            Enrollment existing = existingOpt.get();
            // Nếu đang PENDING → cập nhật orderId mới để retry thanh toán
            if ("PENDING".equals(existing.getPaymentStatus())) {
                existing.setOrderId(orderId);
                Enrollment saved = enrollmentRepository.save(existing);
                log.info("Updated pending enrollment orderId: userId={}, courseId={}, newOrderId={}", userId, courseId, orderId);
                return new EnrollmentDto(saved);
            }
            // Đã PAID/FREE → không cho tạo lại
            throw new Exception("User already enrolled in this course");
        }
        
        Enrollment enrollment = new Enrollment(user, course);
        enrollment.setStatus("PENDING_PAYMENT");
        enrollment.setPaymentStatus("PENDING");
        enrollment.setOrderId(orderId);
        
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Pending enrollment created: userId={}, courseId={}, orderId={}", userId, courseId, orderId);
        return new EnrollmentDto(saved);
    }
    
    /**
     * Get all enrollments for logged-in user
     */
    public Page<EnrollmentDto> getMyEnrollments(Long userId, Pageable pageable) {
        return enrollmentRepository.findByUser_Id(userId, pageable)
                .map(EnrollmentDto::new);
    }
    
    /**
     * Get active enrollments (in progress or enrolled)
     */
    public Page<EnrollmentDto> getActiveEnrollments(Long userId, Pageable pageable) {
        return enrollmentRepository.findActiveEnrollments(userId, pageable)
                .map(EnrollmentDto::new);
    }
    
    /**
     * Get completed enrollments
     */
    public Page<EnrollmentDto> getCompletedEnrollments(Long userId, Pageable pageable) {
        return enrollmentRepository.findCompletedEnrollments(userId, pageable)
                .map(EnrollmentDto::new);
    }
    
    /**
     * Get enrollment by ID
     */
    public EnrollmentDto getEnrollmentById(Long enrollmentId) throws Exception {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new Exception("Enrollment not found"));
        return new EnrollmentDto(enrollment);
    }
    
    /**
     * Check if user is enrolled in course
     */
    public Boolean isUserEnrolled(Long userId, Long courseId) {
        return enrollmentRepository.existsByUser_IdAndCourse_Id(userId, courseId);
    }
    
    /**
     * Lấy trạng thái enrollment chi tiết (bao gồm paymentStatus).
     * Dùng để frontend biết user đã enroll + đã thanh toán chưa.
     */
    public Map<String, Object> getEnrollmentStatus(Long userId, Long courseId) {
        Map<String, Object> result = new HashMap<>();
        var enrollmentOpt = enrollmentRepository.findByUser_IdAndCourse_Id(userId, courseId);
        
        if (enrollmentOpt.isEmpty()) {
            result.put("enrolled", false);
            result.put("paymentStatus", null);
            result.put("status", null);
            return result;
        }
        
        Enrollment enrollment = enrollmentOpt.get();
        result.put("enrolled", true);
        result.put("paymentStatus", enrollment.getPaymentStatus());
        result.put("status", enrollment.getStatus());
        result.put("courseId", courseId);
        return result;
    }
    
    /**
     * Update enrollment progress
     */
    public EnrollmentDto updateProgress(Long enrollmentId, Float progressPercentage) throws Exception {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new Exception("Enrollment not found"));
        
        enrollment.setProgressPercentage(progressPercentage);
        Enrollment updated = enrollmentRepository.save(enrollment);
        
        log.info("Enrollment {} progress updated to {}%", enrollmentId, progressPercentage);
        return new EnrollmentDto(updated);
    }
    
    /**
     * Mark enrollment as completed
     */
    public EnrollmentDto completeEnrollment(Long enrollmentId) throws Exception {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new Exception("Enrollment not found"));
        
        enrollment.setProgressPercentage(100f);
        Enrollment updated = enrollmentRepository.save(enrollment);
        
        log.info("Enrollment {} marked as completed", enrollmentId);
        return new EnrollmentDto(updated);
    }
    
    /**
     * Unenroll user from course
     */
    public void unenrollUserFromCourse(Long userId, Long courseId) throws Exception {
        Enrollment enrollment = enrollmentRepository.findByUser_IdAndCourse_Id(userId, courseId)
                .orElseThrow(() -> new Exception("Enrollment not found"));
        
        enrollmentRepository.delete(enrollment);
        log.info("User {} unenrolled from course {}", userId, courseId);
    }
    
    /**
     * Get course enrollments
     */
    public Page<EnrollmentDto> getCourseEnrollments(Long courseId, Pageable pageable) {
        return enrollmentRepository.findByCourse_Id(courseId, pageable)
                .map(EnrollmentDto::new);
    }
}
