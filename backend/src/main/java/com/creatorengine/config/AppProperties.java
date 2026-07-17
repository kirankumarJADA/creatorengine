package com.creatorengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
    private Brevo brevo = new Brevo();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String v) { this.frontendBaseUrl = v; }

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public Firebase getFirebase() { return firebase; }
    public void setFirebase(Firebase firebase) { this.firebase = firebase; }

    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }

    public Ai getAi() { return ai; }
    public void setAi(Ai ai) { this.ai = ai; }

    public Brevo getBrevo() { return brevo; }
    public void setBrevo(Brevo brevo) { this.brevo = brevo; }

    public static class Brevo {
        private String apiKey;
        private String fromEmail;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getFromEmail() { return fromEmail; }
        public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
    }

    public static class Cors {
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;
        private boolean allowCredentials;
        private long maxAge;

        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String v) { this.allowedOrigins = v; }
        public String getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(String v) { this.allowedMethods = v; }
        public String getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(String v) { this.allowedHeaders = v; }
        public boolean getAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean v) { this.allowCredentials = v; }
        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long v) { this.maxAge = v; }
    }

    public static class Security {
        private Jwt jwt = new Jwt();
        public Jwt getJwt() { return jwt; }
        public void setJwt(Jwt jwt) { this.jwt = jwt; }

        public static class Jwt {
            private String secret;
            private String issuer;
            private long accessTokenExpirationMs;
            private long refreshTokenExpirationMs;

            public String getSecret() { return secret; }
            public void setSecret(String v) { this.secret = v; }
            public String getIssuer() { return issuer; }
            public void setIssuer(String v) { this.issuer = v; }
            public long getAccessTokenExpirationMs() { return accessTokenExpirationMs; }
            public void setAccessTokenExpirationMs(long v) { this.accessTokenExpirationMs = v; }
            public long getRefreshTokenExpirationMs() { return refreshTokenExpirationMs; }
            public void setRefreshTokenExpirationMs(long v) { this.refreshTokenExpirationMs = v; }
        }
    }

    public static class Firebase {
        private String projectId;
        private String webApiKey;
        private String credentialsPath;
        private String credentialsJson;
        private String passwordResetRedirectUrl;

        public String getProjectId() { return projectId; }
        public void setProjectId(String v) { this.projectId = v; }
        public String getWebApiKey() { return webApiKey; }
        public void setWebApiKey(String v) { this.webApiKey = v; }
        public String getCredentialsPath() { return credentialsPath; }
        public void setCredentialsPath(String v) { this.credentialsPath = v; }
        public String getCredentialsJson() { return credentialsJson; }
        public void setCredentialsJson(String v) { this.credentialsJson = v; }
        public String getPasswordResetRedirectUrl() { return passwordResetRedirectUrl; }
        public void setPasswordResetRedirectUrl(String v) { this.passwordResetRedirectUrl = v; }
    }

    public static class Meta {
        private String appId;
        private String appSecret;
        private String verifyToken;
        private String redirectUri;
        private String successRedirectUri;
        private String graphApiVersion = "v19.0";
        private String scopes =
                "instagram_basic,instagram_manage_comments,instagram_manage_messages,"
                        + "pages_show_list,pages_read_engagement";

        public String getAppId() { return appId; }
        public void setAppId(String v) { this.appId = v; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String v) { this.appSecret = v; }
        public String getVerifyToken() { return verifyToken; }
        public void setVerifyToken(String v) { this.verifyToken = v; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String v) { this.redirectUri = v; }
        public String getSuccessRedirectUri() { return successRedirectUri; }
        public void setSuccessRedirectUri(String v) { this.successRedirectUri = v; }
        public String getGraphApiVersion() { return graphApiVersion; }
        public void setGraphApiVersion(String v) { this.graphApiVersion = v; }
        public String getScopes() { return scopes; }
        public void setScopes(String v) { this.scopes = v; }
    }

    public static class Ai {
        private Openai openai = new Openai();
        private Gemini gemini = new Gemini();
        public Openai getOpenai() { return openai; }
        public void setOpenai(Openai openai) { this.openai = openai; }
        public Gemini getGemini() { return gemini; }
        public void setGemini(Gemini gemini) { this.gemini = gemini; }

        public static class Openai {
            private String apiKey;
            private String model = "gpt-4o-mini";
            private String baseUrl = "https://api.openai.com/v1";
            private int timeoutMs = 20000;

            public String getApiKey() { return apiKey; }
            public void setApiKey(String v) { this.apiKey = v; }
            public String getModel() { return model; }
            public void setModel(String v) { this.model = v; }
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String v) { this.baseUrl = v; }
            public int getTimeoutMs() { return timeoutMs; }
            public void setTimeoutMs(int v) { this.timeoutMs = v; }
        }

        /** Google Gemini — powers the AI FAQ auto-answer feature. */
        public static class Gemini {
            private String apiKey;
            private String model = "gemini-2.0-flash";
            private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
            private int timeoutMs = 20000;

            public String getApiKey() { return apiKey; }
            public void setApiKey(String v) { this.apiKey = v; }
            public String getModel() { return model; }
            public void setModel(String v) { this.model = v; }
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String v) { this.baseUrl = v; }
            public int getTimeoutMs() { return timeoutMs; }
            public void setTimeoutMs(int v) { this.timeoutMs = v; }
        }
    }
}