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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates JWT access + refresh tokens.
 *
 * <p>Two token kinds, distinguished by the {@code typ} claim:</p>
 * <ul>
 *   <li><b>access</b>  — short-lived, sent with every API request</li>
 *   <li><b>refresh</b> — long-lived, only valid at {@code /auth/refresh}</li>
 * </ul>
 *
 * <p>The {@code typ} check makes it impossible for a refresh token to
 * be accepted on a normal endpoint, even if leaked.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String TYPE_OAUTH_STATE = "oauth_state";

    /** Five minutes is plenty for a user to bounce through Meta OAuth. */
    private static final long OAUTH_STATE_TTL_MS = 5 * 60 * 1000L;

    private final AppProperties props;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = props.getSecurity().getJwt().getSecret();
        if (secret == null || secret.isBlank() || secret.startsWith("CHANGE_ME")) {
            log.warn("⚠️  JWT_SECRET is not configured. Set a strong base64 secret before deploy.");
        }
        // Accept either base64 or raw input; fall through to raw on decode failure.
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes();
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // ─── Issuance ────────────────────────────────────────────────
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

    // ─── Validation / parsing ────────────────────────────────────
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

    // ─── OAuth state tokens ──────────────────────────────────────
    /*
     * Used to carry the user's UID through Meta's OAuth flow as the
     * `state` parameter. We can't put a regular access token in there
     * because (a) it's long-lived and (b) it would be exposed in URLs
     * and browser history. A short-lived, type-distinct token is the
     * right tool.
     */

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

    /** Returns the UID embedded in a state token, or null if invalid/expired. */
    public String parseOAuthStateUserId(String token) {
        try {
            Claims c = parse(token);
            if (!TYPE_OAUTH_STATE.equals(c.get(CLAIM_TYPE, String.class))) return null;
            return c.getSubject();
        } catch (Exception ex) {
            log.debug("Invalid OAuth state token: {}", ex.getMessage());
            return null;
        }
    }
}
