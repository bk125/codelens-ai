package com.codereviewer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationCode(String email, String code) {
        String content = "Welcome to CodeLens AI! Your verification code is: <br><br>" +
                         "<div style='font-family:\"Courier New\", monospace; font-size: 32px; font-weight: bold; color: #00f5d4; background: #0f172a; display: inline-block; padding: 10px 20px; border-radius: 8px; letter-spacing: 5px;'>" +
                         code + "</div><br><br>" +
                         "This code will expire in 15 minutes.";

        sendHtmlEmail(email, "Your Verification Code - CodeLens AI", content);
    }

    public void sendPasswordResetEmail(String email, String token) {
        String resetLink = "http://localhost:4200/reset-password?token=" + token;
        String content = "You requested a password reset for your CodeLens AI account. <br><br>" +
                         "Click the button below to securely reset your password. This link will expire in 1 hour.";

        String buttonHtml = "<a href='" + resetLink + "' style='background: #00f5d4; color: #000; padding: 12px 24px; text-decoration: none; font-weight: bold; border-radius: 8px; display: inline-block;'>Reset Password</a>";

        sendHtmlEmail(email, "Reset Your Password - CodeLens AI", content, buttonHtml);
    }

    private void sendHtmlEmail(String to, String subject, String content) {
        sendHtmlEmail(to, subject, content, null);
    }

    private void sendHtmlEmail(String to, String subject, String content, String buttonHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom("noreply@codereviewer.com");
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlBody = "<html><body style='font-family: \"Inter\", -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 0;'>" +
                              "<table align='center' style='max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 4px 12px rgba(0,0,0,0.05);'>" +
                              "<tr><td style='padding: 40px 20px; text-align: center;'>" +
                              "<div style='display: inline-block; width: 48px; height: 48px; background: linear-gradient(135deg, #00f5d4, #7c5cfc); color: #000; font-weight: 800; line-height: 48px; font-size: 1.2rem; clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%); text-align: center;'>CL</div>" +
                              "<h1 style='color: #0f172a; font-size: 24px; margin-top: 20px;'>CodeLens AI</h1>" +
                              "<div style='color: #475569; font-size: 16px; line-height: 1.6; margin-top: 20px;'>" + content + "</div>" +
                              (buttonHtml != null ? "<div style='margin-top: 30px;'>" + buttonHtml + "</div>" : "") +
                              "</td></tr>" +
                              "<tr><td style='background-color: #f1f5f9; padding: 20px; text-align: center; color: #94a3b8; font-size: 12px;'>© 2025 CodeLens AI. All rights reserved.</td></tr>" +
                              "</table></body></html>";

            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("HTML email sent to {}", to);
        } catch (Exception e) {
            logger.error("Error sending HTML email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }
}
