package com.creatorengine.security;

import com.creatorengine.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Lightweight {@link UserDetails} stored in the SecurityContext for the
 * lifetime of a request.
 *
 * <p>Carries the Firebase UID (as {@code id}) and the user's roles —
 * everything controllers need to authorise an action without doing
 * another Firestore round-trip.</p>
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final String id;        // Firebase UID
    private final String email;
    private final List<Role> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles == null
                ? List.of()
                : roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .toList();
    }

    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
