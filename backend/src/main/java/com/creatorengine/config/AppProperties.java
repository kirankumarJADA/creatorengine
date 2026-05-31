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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Firebase getFirebase() {
        return firebase;
    }

    public void setFirebase(Firebase firebase) {
        this.firebase = firebase;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public static class Cors {
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;
        private boolean allowCredentials;
        private long maxAge;

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(String allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public String getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(String allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public boolean getAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class Security {
        private Jwt jwt = new Jwt();

        public Jwt getJwt() {
            return jwt;
        }

        public void setJwt(Jwt jwt) {
            this.jwt = jwt;
        }

        public static class Jwt {
            private String secret;
            private String issuer;
            private long accessTokenExpirationMs;
            private long refreshTokenExpirationMs;

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public String getIssuer() {
                return issuer;
            }

            public void setIssuer(String issuer) {
                this.issuer = issuer;
            }

            public long getAccessTokenExpirationMs() {
                return accessTokenExpirationMs;
            }

            public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
                this.accessTokenExpirationMs = accessTokenExpirationMs;
            }

            public long getRefreshTokenExpirationMs() {
                return refreshTokenExpirationMs;
            }

            public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
                this.refreshTokenExpirationMs = refreshTokenExpirationMs;
            }
        }
    }

    public static class Firebase {
        private String projectId;
        private String webApiKey;
        private String credentialsPath;
        private String credentialsJson;
        private String passwordResetRedirectUrl;

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getWebApiKey() {
            return webApiKey;
        }

        public void setWebApiKey(String webApiKey) {
            this.webApiKey = webApiKey;
        }

        public String getCredentialsPath() {
            return credentialsPath;
        }

        public void setCredentialsPath(String credentialsPath) {
            this.credentialsPath = credentialsPath;
        }

        public String getCredentialsJson() {
            return credentialsJson;
        }

        public void setCredentialsJson(String credentialsJson) {
            this.credentialsJson = credentialsJson;
        }

        public String getPasswordResetRedirectUrl() {
            return passwordResetRedirectUrl;
        }

        public void setPasswordResetRedirectUrl(String passwordResetRedirectUrl) {
            this.passwordResetRedirectUrl = passwordResetRedirectUrl;
        }
    }

    public static class Meta {
        private String appId;
        private String appSecret;
        private String verifyToken;
        private String redirectUri;
        private String successRedirectUri;
        private String graphApiVersion = "v19.0";
        private String scopes =
                "instagram_basic,pages_show_list,pages_read_engagement,"
                        + "instagram_manage_comments,instagram_manage_messages,"
                        + "pages_manage_metadata,business_management";

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getVerifyToken() {
            return verifyToken;
        }

        public void setVerifyToken(String verifyToken) {
            this.verifyToken = verifyToken;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getSuccessRedirectUri() {
            return successRedirectUri;
        }

        public void setSuccessRedirectUri(String successRedirectUri) {
            this.successRedirectUri = successRedirectUri;
        }

        public String getGraphApiVersion() {
            return graphApiVersion;
        }

        public void setGraphApiVersion(String graphApiVersion) {
            this.graphApiVersion = graphApiVersion;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }
    }

    public static class Ai {
        private Openai openai = new Openai();

        public Openai getOpenai() {
            return openai;
        }

        public void setOpenai(Openai openai) {
            this.openai = openai;
        }

        public static class Openai {
            private String apiKey;
            private String model = "gpt-4o-mini";
            private String baseUrl = "https://api.openai.com/v1";
            private int timeoutMs = 20000;

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public int getTimeoutMs() {
                return timeoutMs;
            }

            public void setTimeoutMs(int timeoutMs) {
                this.timeoutMs = timeoutMs;
            }
        }
    }
}