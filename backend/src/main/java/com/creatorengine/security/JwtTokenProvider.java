package com.creatorengine.security;

import com.creatorengine.auth.entity.Role;
import com.creatorengine.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String TYPE_OAUTH_STATE = "oauth_state";

    private static final long OAUTH_STATE_TTL_MS = 5 * 60 * 1000L;

    private final AppProperties props;
    private SecretKey signingKey;

    public JwtTokenProvider(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        String secret = props.getSecurity().getJwt().getSecret();

        if (secret == null || secret.isBlank() || secret.startsWith("CHANGE_ME")) {
            log.warn("JWT_SECRET is not configured. Set a strong base64 secret before deploy.");
        }

        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String uid, String email, List<Role> roles) {
        return build(uid, email, roles, TYPE_ACCESS,
                props.getSecurity().getJwt().getAccessTokenExpirationMs());
    }

    public String generateRefreshToken(String uid, String email) {
        return build(uid, email, List.of(), TYPE_REFRESH,
                props.getSecurity().getJwt().getRefreshTokenExpirationMs());
    }

    private String build(String uid, String email, List<Role> roles, String type, long ttlMs) {
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>();

        claims.put(CLAIM_EMAIL, email);
        claims.put(CLAIM_TYPE, type);

        if (roles != null && !roles.isEmpty()) {
            claims.put(CLAIM_ROLES, roles.stream().map(Enum::name).toList());
        }

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getSecurity().getJwt().getIssuer())
                .subject(uid)
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(props.getSecurity().getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValidAccessToken(String token) {
        return isValid(token, TYPE_ACCESS);
    }

    public boolean isValidRefreshToken(String token) {
        return isValid(token, TYPE_REFRESH);
    }

    private boolean isValid(String token, String expectedType) {
        try {
            Claims c = parse(token);
            return expectedType.equals(c.get(CLAIM_TYPE, String.class));
        } catch (ExpiredJwtException ex) {
            log.debug("JWT expired: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
        }

        return false;
    }

    public String generateOAuthStateToken(String uid) {
        Date now = new Date();
        Map<String, Object> claims = new HashMap<>();

        claims.put(CLAIM_TYPE, TYPE_OAUTH_STATE);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getSecurity().getJwt().getIssuer())
                .subject(uid)
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + OAUTH_STATE_TTL_MS))
                .signWith(signingKey)
                .compact();
    }

    public String parseOAuthStateUserId(String token) {
        try {
            Claims c = parse(token);

            if (!TYPE_OAUTH_STATE.equals(c.get(CLAIM_TYPE, String.class))) {
                return null;
            }

            return c.getSubject();
        } catch (Exception ex) {
            log.debug("Invalid OAuth state token: {}", ex.getMessage());
            return null;
        }
    }
}