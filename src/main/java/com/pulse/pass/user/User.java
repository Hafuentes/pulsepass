package com.pulse.pass.user;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Identidad de dominio de un usuario (sin credenciales de seguridad, ver R-004).
 * Relacion 1:1 con UserProfile (BR-004): a lo sumo un perfil por usuario.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "username", nullable = false, unique = true, length = 80)
    private String username;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Lado inverso; el dueno de la relacion 1:1 es UserProfile.user (FK unica user_id). */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private UserProfile profile;

    protected User() {
        // Requerido por JPA
    }

    public User(String username, String email, boolean active) {
        this.username = username;
        this.email = email;
        this.active = active;
    }

    /** Mantiene sincronizados ambos lados de la relacion 1:1. */
    public void assignProfile(UserProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UserProfile getProfile() {
        return profile;
    }

    @Override
    public String toString() {
        return "User{id=%d, username='%s', email='%s'}".formatted(id, username, email);
    }
}
