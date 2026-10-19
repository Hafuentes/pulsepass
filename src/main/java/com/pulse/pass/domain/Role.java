package com.pulse.pass.domain;

/** Roles de la Fase 4. En Spring Security se exponen como ROLE_USER, ROLE_STAFF y ROLE_ADMIN. */
public enum Role {
    USER,
    STAFF,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
