package com.creatorengine.auth.service;

import com.creatorengine.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String FROM_NAME = "CreatorEngine";

    private final AppProperties props;
    private final RestClient restClient;

    public ResendEmailService(AppProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(BREVO_API_URL)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public void sendOtpEmail(String toEmail, String otp) {
        send(toEmail, "Your CreatorEngine verification code: " + otp, buildOtpHtml(otp));
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        send(toEmail, "Reset your CreatorEngine password", buildResetHtml(resetLink));
    }

    /**
     * Generic notification email — used by AI Autopilot (#15) to alert the
     * account owner when a conversation is escalated or a lead qualifies.
     */
    public void sendOwnerNotification(String toEmail, String subject, String bodyText) {
        if (toEmail == null || toEmail.isBlank()) return;
        send(toEmail, subject, buildNotificationHtml(subject, bodyText));
    }

    private String buildNotificationHtml(String heading, String bodyText) {
        String safeBody = bodyText == null ? "" : bodyText.replace("\n", "<br/>");
        return "<div style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;"
                + "max-width:480px;margin:0 auto;padding:40px 24px;background:#ffffff;\">"
                + "<div style=\"margin-bottom:32px;\">"
                + "<span style=\"font-size:22px;font-weight:700;color:#7c3aed;\">CreatorEngine</span>"
                + "</div>"
                + "<h1 style=\"font-size:22px;font-weight:700;color:#111827;margin:0 0 16px;\">" + heading + "</h1>"
                + "<p style=\"font-size:15px;color:#374151;line-height:1.6;margin:0 0 32px;\">" + safeBody + "</p>"
                + "<div style=\"border-top:1px solid #f3f4f6;margin:32px 0;\"></div>"
                + "<p style=\"font-size:12px;color:#d1d5db;margin:0;text-align:center;\">"
                + "CreatorEngine &middot; AI Autopilot</p>"
                + "</div>";
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
        String apiKey = props.getBrevo().getApiKey();
        String fromEmail = props.getBrevo().getFromEmail();

        if (apiKey == null || apiKey.isBlank()) {
            log.error("BREVO_API_KEY not configured — email NOT sent to {}", to);
            return;
        }

        Map<String, Object> body = Map.of(
                "sender", Map.of("name", FROM_NAME, "email", fromEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", html
        );

        try {
            restClient.post()
                    .header("api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Brevo email sent to={} subject='{}'", to, subject);
        } catch (Exception ex) {
            log.error("Brevo email failed to={} subject='{}': {}", to, subject, ex.getMessage());
        }
    }
}