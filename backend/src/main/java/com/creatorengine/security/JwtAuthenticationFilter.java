package com.creatorengine.security;

import com.creatorengine.auth.entity.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Per-request JWT filter.
 *
 * <ol>
 *   <li>Pull {@code Authorization: Bearer &lt;token&gt;}.</li>
 *   <li>Validate as an <b>access</b> token (refresh tokens are rejected
 *       here — they only flow through /auth/refresh).</li>
 *   <li>Build a {@link UserPrincipal} from the claims and place it in
 *       the SecurityContext.</li>
 * </ol>
 *
 * <p>Failures are silently swallowed — the request continues
 * unauthenticated and the SecurityConfig's entry point produces the
 * 401 response.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(req);
        if (StringUtils.hasText(token) && tokenProvider.isValidAccessToken(token)) {
            try {
                Claims claims = tokenProvider.parse(token);
                String uid = claims.getSubject();
                String email = claims.get(JwtTokenProvider.CLAIM_EMAIL, String.class);
                List<Role> roles = readRoles(claims);

                UserPrincipal principal = new UserPrincipal(uid, email, roles);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                log.debug("Failed to set auth from JWT: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(req, res);
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Role> readRoles(Claims claims) {
        Object raw = claims.get(JwtTokenProvider.CLAIM_ROLES);
        if (!(raw instanceof List<?> list)) return List.of(Role.USER);
        return list.stream()
                .map(Object::toString)
                .map(name -> {
                    try { return Role.valueOf(name); }
                    catch (IllegalArgumentException e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
