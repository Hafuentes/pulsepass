package com.pulse.pass.user;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository de soporte para UserProfile. No esta en el listado minimo del PRD,
 * pero facilita probar directamente la relacion 1:1 (FR-USR-003) y su restriccion UNIQUE.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
