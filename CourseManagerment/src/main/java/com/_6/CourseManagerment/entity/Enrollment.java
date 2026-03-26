package com._6.CourseManagerment.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Enrollment Entity - Represents a user's enrollment in a course
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "course_id"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Enrollment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @Column(nullable = false)
    private LocalDateTime enrollmentDate;
    
    @Column
    private LocalDateTime completionDate;
    
    @Column
    private Float progressPercentage = 0f;
    
    @Column
    private String status; // PENDING_PAYMENT, ENROLLED, IN_PROGRESS, COMPLETED
    
    @Column
    private String paymentStatus; // PENDING, PAID, FREE
    
    @Column(unique = true)
    private String orderId; // Mã đơn hàng liên kết với MoMo payment
    
    @Column
    private LocalDateTime lastAccessedDate;
    
    @PrePersist
    protected void onCreate() {
        this.enrollmentDate = LocalDateTime.now();
        this.lastAccessedDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ENROLLED";
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = "FREE";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.lastAccessedDate = LocalDateTime.now();
    }
    
    // Constructors
    public Enrollment() {}
    
    public Enrollment(User user, Course course) {
        this.user = user;
        this.course = course;
        this.progressPercentage = 0f;
        this.status = "ENROLLED";
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Course getCourse() {
        return course;
    }
    
    public void setCourse(Course course) {
        this.course = course;
    }
    
    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }
    
    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    
    public LocalDateTime getCompletionDate() {
        return completionDate;
    }
    
    public void setCompletionDate(LocalDateTime completionDate) {
        this.completionDate = completionDate;
    }
    
    public Float getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Float progressPercentage) {
        if (progressPercentage >= 100f) {
            this.progressPercentage = 100f;
            this.status = "COMPLETED";
            this.completionDate = LocalDateTime.now();
        } else {
            this.progressPercentage = progressPercentage;
            if (progressPercentage > 0f) {
                this.status = "IN_PROGRESS";
            }
        }
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getLastAccessedDate() {
        return lastAccessedDate;
    }
    
    public void setLastAccessedDate(LocalDateTime lastAccessedDate) {
        this.lastAccessedDate = lastAccessedDate;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
