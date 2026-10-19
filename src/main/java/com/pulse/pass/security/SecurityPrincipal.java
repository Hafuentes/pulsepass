package com.pulse.pass.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Identidad autenticada. Se construye SIEMPRE desde la base de datos (usuario, estado y roles actuales),
 * nunca desde claims del JWT (FR-SECAUTH-009).
 */
public final class SecurityPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public SecurityPrincipal(Long userId, String username, String email, String passwordHash,
                             boolean active, List<GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = List.copyOf(authorities);
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean hasRole(String role) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    /** Nunca incluye el hash. */
    @Override
    public String toString() {
        return "SecurityPrincipal{userId=%d, username='%s'}".formatted(userId, username);
    }
}
