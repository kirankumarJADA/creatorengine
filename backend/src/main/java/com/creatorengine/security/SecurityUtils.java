package com.creatorengine.security;

import com.creatorengine.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static helper to retrieve the current authenticated user from the
 * SecurityContext. Useful in services that don't have access to the
 * controller's {@code @AuthenticationPrincipal}.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof UserPrincipal up)) {
            throw new UnauthorizedException("Not authenticated.");
        }
        return up;
    }

    public static String getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
