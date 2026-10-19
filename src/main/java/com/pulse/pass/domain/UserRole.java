package com.pulse.pass.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Rol asignado a un usuario. PK compuesta (user_id, role). */
@Entity
@Table(name = "user_roles")
@IdClass(UserRole.Key.class)
public class UserRole {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private Role role;

    protected UserRole() {
        // Requerido por JPA
    }

    public UserRole(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long userId;
        private Role role;

        public Key() {
        }

        public Key(Long userId, Role role) {
            this.userId = userId;
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key other)) {
                return false;
            }
            return Objects.equals(userId, other.userId) && role == other.role;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, role);
        }
    }
}
