package com.pulse.pass.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Credencial 1:1 con User (PK = user_id). Se mantiene separada para que el hash nunca llegue a un DTO. */
@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    protected UserCredential() {
        // Requerido por JPA
    }

    public UserCredential(Long userId, String passwordHash, LocalDateTime passwordChangedAt) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    /** Nunca incluye el hash. */
    @Override
    public String toString() {
        return "UserCredential{userId=%d}".formatted(userId);
    }
}
