package com.creatorengine.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public ResendEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        String html = buildOtpHtml(otp);
        send(toEmail, "Your CreatorEngine verification code: " + otp, html);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String html = buildResetHtml(resetLink);
        send(toEmail, "Reset your CreatorEngine password", html);
    }

    private String buildOtpHtml(String otp) {
        return "<div style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
                + "max-width:480px;margin:0 auto;padding:40px 24px;background:#ffffff;\">"
                + "<div style=\"margin-bottom:32px;\">"
                + "<span style=\"font-size:22px;font-weight:700;color:#7c3aed;\">CreatorEngine</span>"
                + "</div>"
                + "<h1 style=\"font-size:24px;font-weight:700;color:#111827;margin:0 0 8px;\">Verify your email</h1>"
                + "<p style=\"font-size:15px;color:#6b7280;margin:0 0 32px;\">"
                + "Enter this code to complete your signup. It expires in 10 minutes.</p>"
                + "<div style=\"background:#f5f3ff;border:2px solid #7c3aed;"
                + "border-radius:12px;padding:24px;text-align:center;margin-bottom:32px;\">"
                + "<div style=\"font-size:42px;font-weight:800;letter-spacing:12px;color:#7c3aed;\">"
                + otp
                + "</div></div>"
                + "<p style=\"font-size:13px;color:#9ca3af;margin:0 0 8px;\">"
                + "If you didn't create a CreatorEngine account, ignore this email.</p>"
                + "<p style=\"font-size:13px;color:#9ca3af;margin:0;\">"
                + "This code expires in <strong>10 minutes</strong> and can only be used once.</p>"
                + "<div style=\"border-top:1px solid #f3f4f6;margin:32px 0;\"></div>"
                + "<p style=\"font-size:12px;color:#d1d5db;margin:0;text-align:center;\">"
                + "CreatorEngine &middot; Instagram Automation</p>"
                + "</div>";
    }

    private String buildResetHtml(String resetLink) {
        return "<div style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
                + "max-width:480px;margin:0 auto;padding:40px 24px;background:#ffffff;\">"
                + "<div style=\"margin-bottom:32px;\">"
                + "<span style=\"font-size:22px;font-weight:700;color:#7c3aed;\">CreatorEngine</span>"
                + "</div>"
                + "<h1 style=\"font-size:24px;font-weight:700;color:#111827;margin:0 0 8px;\">Reset your password</h1>"
                + "<p style=\"font-size:15px;color:#6b7280;margin:0 0 32px;\">"
                + "We received a request to reset your password. "
                + "Click the button below to choose a new one.</p>"
                + "<div style=\"text-align:center;margin-bottom:32px;\">"
                + "<a href=\"" + resetLink + "\" "
                + "style=\"display:inline-block;background:#7c3aed;color:#ffffff;"
                + "font-size:15px;font-weight:600;text-decoration:none;"
                + "padding:14px 32px;border-radius:8px;\">Reset Password</a>"
                + "</div>"
                + "<p style=\"font-size:13px;color:#9ca3af;margin:0 0 8px;\">"
                + "Or copy and paste this link into your browser:</p>"
                + "<p style=\"font-size:12px;color:#7c3aed;word-break:break-all;margin:0 0 32px;\">"
                + resetLink + "</p>"
                + "<p style=\"font-size:13px;color:#9ca3af;margin:0;\">"
                + "This link expires in <strong>1 hour</strong>. "
                + "If you didn't request this, ignore this email.</p>"
                + "<div style=\"border-top:1px solid #f3f4f6;margin:32px 0;\"></div>"
                + "<p style=\"font-size:12px;color:#d1d5db;margin:0;text-align:center;\">"
                + "CreatorEngine &middot; Instagram Automation</p>"
                + "</div>";
    }

    private void send(String to, String subject, String html) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.error("MAIL_USERNAME not configured — email NOT sent to {}", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "CreatorEngine");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Gmail email sent to={} subject='{}'", to, subject);
        } catch (Exception ex) {
            log.error("Gmail email failed to={} subject='{}': {}", to, subject, ex.getMessage());
        }
    }
}