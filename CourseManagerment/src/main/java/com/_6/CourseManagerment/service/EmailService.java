package com._6.CourseManagerment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Email Service - Handles sending emails
 * Supports both text and HTML emails
 */
@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    /**
     * Send simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
    
    /**
     * Send HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            
            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }
    
    /**
     * Send registration success email
     */
    public void sendRegistrationSuccessEmail(String toEmail, String fullName) {
        String subject = "Welcome to Course Management System!";
        String htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2>Welcome, %s!</h2>
                    <p>Your account has been successfully created on our Course Management System.</p>
                    <p>You can now:</p>
                    <ul>
                        <li>Browse and enroll in courses</li>
                        <li>Track your learning progress</li>
                        <li>Join live learning sessions</li>
                        <li>Connect with other learners</li>
                    </ul>
                    <p>If you have any questions, feel free to contact our support team.</p>
                    <br/>
                    <p>Happy Learning!</p>
                    <p>Regards,<br/>Course Management Team</p>
                </body>
            </html>
            """.formatted(fullName);
        
        sendHtmlEmail(toEmail, subject, htmlContent);
    }
    
    /**
     * Send payment success email
     */
    public void sendPaymentSuccessEmail(String toEmail, String fullName, String courseName, Double amount) {
        String subject = "Payment Confirmation - " + courseName;
        String htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2>Payment Successful!</h2>
                    <p>Dear %s,</p>
                    <p>Your payment has been processed successfully.</p>
                    <table style="border-collapse: collapse; width: 100%; margin: 20px 0;">
                        <tr style="background-color: #f2f2f2;">
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Course Name</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Amount Paid</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">$%.2f</td>
                        </tr>
                        <tr style="background-color: #f2f2f2;">
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Status</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">Confirmed</td>
                        </tr>
                    </table>
                    <p>You can now access the course materials. Happy learning!</p>
                    <br/>
                    <p>Regards,<br/>Course Management Team</p>
                </body>
            </html>
            """.formatted(fullName, courseName, amount);
        
        sendHtmlEmail(toEmail, subject, htmlContent);
    }
    
    /**
     * Send password reset OTP email
     */
    public void sendPasswordResetEmail(String toEmail, String fullName, String otp) {
        String subject = "Password Reset Request - Course Management System";
        String htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2>Password Reset Request</h2>
                    <p>Dear %s,</p>
                    <p>We received a request to reset your password. If you didn't make this request, you can ignore this email.</p>
                    <p>To reset your password, use the following OTP (One-Time Password):</p>
                    <p style="background-color: #f2f2f2; padding: 15px; text-align: center; font-size: 20px; font-weight: bold; letter-spacing: 2px;">
                        %s
                    </p>
                    <p>This OTP is valid for 15 minutes.</p>
                    <p>For security reasons, never share your OTP with anyone.</p>
                    <br/>
                    <p>Regards,<br/>Course Management Team</p>
                </body>
            </html>
            """.formatted(fullName, otp);
        
        sendHtmlEmail(toEmail, subject, htmlContent);
    }
}
