package com.creatorengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe binding for the {@code app.*} property tree.
 *
 * <p>Centralising config in a single bean means consumers inject just
 * this and read what they need — no scattering of {@code @Value}
 * annotations and no stringly-typed property keys.</p>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name;
    private String frontendBaseUrl;
    private Cors cors = new Cors();
    private Security security = new Security();
    private Firebase firebase = new Firebase();
    private Meta meta = new Meta();
    private Ai ai = new Ai();

    @Getter @Setter
    public static class Cors {
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;
        private boolean allowCredentials;
        private long maxAge;
    }

    @Getter @Setter
    public static class Security {
        private Jwt jwt = new Jwt();

        @Getter @Setter
        public static class Jwt {
            private String secret;
            private String issuer;
            private long accessTokenExpirationMs;
            private long refreshTokenExpirationMs;
        }
    }

    @Getter @Setter
    public static class Firebase {
        private String projectId;
        private String webApiKey;
        private String credentialsPath;
        private String credentialsJson;
        private String passwordResetRedirectUrl;
    }

    @Getter @Setter
    public static class Meta {
        /** Facebook/Meta App ID. */
        private String appId;
        /** Facebook/Meta App Secret — never log this. */
        private String appSecret;
        /** Token Meta echoes during the webhook verification handshake. */
        private String verifyToken;
        /** OAuth redirect URI registered with Meta. Must point at the backend's /callback. */
        private String redirectUri;
        /** Where to send the user after the OAuth callback completes. */
        private String successRedirectUri;
        /** Graph API version. Default v19.0 — bump as Meta releases new versions. */
        private String graphApiVersion = "v19.0";
        /** Comma-separated OAuth scopes. */
        private String scopes =
                "instagram_basic,instagram_manage_comments,instagram_manage_messages,"
                + "pages_show_list,pages_read_engagement,business_management";
    }

    @Getter @Setter
    public static class Ai {
        /**
         * OpenAI provider config. Optional — if {@link Openai#apiKey} is
         * blank, the OpenAI provider doesn't register and the service
         * falls back to template-based suggestions. This keeps the
         * AI assistant working in dev without any external dependency.
         */
        private Openai openai = new Openai();

        @Getter @Setter
        public static class Openai {
            private String apiKey;
            private String model = "gpt-4o-mini";
            private String baseUrl = "https://api.openai.com/v1";
            /** Total request timeout in milliseconds. */
            private int timeoutMs = 20000;
        }
    }
}
