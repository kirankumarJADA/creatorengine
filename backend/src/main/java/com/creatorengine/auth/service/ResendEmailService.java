package com.creatorengine.auth.service;

import com.creatorengine.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final String FROM = "CreatorEngine <onboarding@resend.dev>";

    private final AppProperties props;
    private final RestClient restClient;

    public ResendEmailService(AppProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(RESEND_API_URL)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void sendOtpEmail(String toEmail, String otp) {
        String html = """
            <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
                        max-width:480px;margin:0 auto;padding:40px 24px;background:#ffffff;">
              <div style="margin-bottom:32px;">
                <span style="font-size:22px;font-weight:700;color:#7c3aed;">
                  CreatorEngine
                </span>
              </div>
              <h1 style="font-size:24px;font-weight:700;color:#111827;margin:0 0 8px;">
                Verify your email
              </h1>
              <p style="font-size:15px;color:#6b7280;margin:0 0 32px;">
                Enter this code to complete your signup. It expires in 10 minutes.
              </p>
              <div style="background:#f5f3ff;border:2px solid #7c3aed;
                          border-radius:12px;padding:24px;
                          text-align:center;margin-bottom:32px;">
                <div style="font-size:42px;font-weight:800;
                            letter-spacing:12px;color:#7c3aed;">
                  %s
                </div>
              </div>
              <p style="font-size:13px;color:#9ca3af;margin:0 0 8px;">
                If you didn't create a CreatorEngine account, ignore this email.
              </p>
              <p style="font-size:13px;color:#9ca3af;margin:0;">
                This code expires in <strong>10 minutes</strong> and can only be used once.
              </p>
              <div style="border-top:1px solid #f3f4f6;margin:32px 0;"></div>
              <p style="font-size:12px;color:#d1d5db;margin:0;text-align:center;">
                CreatorEngine · Instagram Automation
              </p>
            </div>
            """.formatted(otp);

        send(toEmail, "Your CreatorEngine verification code: " + otp, html);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String html = """
            <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
                        max-width:480px;margin:0 auto;padding:40px 24px;background:#ffffff;">
              <div style="margin-bottom:32px;">
                <span