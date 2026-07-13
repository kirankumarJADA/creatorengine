package com.creatorengine.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Cloudinary is used ONLY for storing images attached to automation DMs
 * (Send Image in DM feature). Everything else in the app (auth, database,
 * automations) continues to use Firebase exactly as before - this is a
 * narrow, single-purpose addition, not a replacement for Firebase.
 *
 * Free tier: 25GB storage / 25GB bandwidth per month, no credit card
 * required. Sign up at cloudinary.com and copy Cloud Name, API Key, and
 * API Secret from your dashboard into these environment variables:
 *   CLOUDINARY_CLOUD_NAME
 *   CLOUDINARY_API_KEY
 *   CLOUDINARY_API_SECRET
 */
@Configuration
public class CloudinaryConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    /**
     * Returns null (instead of throwing) if credentials aren't configured
     * yet, so a missing/incomplete Cloudinary setup never crashes the whole
     * backend - it only disables the optional "Send Image in DM" feature
     * until credentials are added. Same safety pattern as every other
     * optional integration in this app.
     */
    @Bean
    public Cloudinary cloudinary() {
        if (!StringUtils.hasText(cloudName)
                || !StringUtils.hasText(apiKey)
                || !StringUtils.hasText(apiSecret)) {
            log.warn("Cloudinary is not configured (CLOUDINARY_CLOUD_NAME / "
                    + "CLOUDINARY_API_KEY / CLOUDINARY_API_SECRET missing). "
                    + "Send Image in DM will be unavailable until these are set.");
            return null;
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));

        log.info("Cloudinary connected (cloud_name='{}').", cloudName);
        return cloudinary;
    }
}