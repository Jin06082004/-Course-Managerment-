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
     * Enroll user in a course
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
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        
        // Update course student count
        course.setStudentCount(enrollmentRepository.countByCourse_Id(courseId));
        courseRepository.save(course);
        
        log.info("User {} enrolled in course {}", userId, courseId);
        return new EnrollmentDto(savedEnrollment);
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
