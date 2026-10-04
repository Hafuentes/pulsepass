package com.pulse.pass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pulse.pass.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    /** FR-USR-002: recuperar por username unico. */
    Optional<User> findByUsername(String username);

    /** Estrategia de consultas sec.14: buscar usuario por email ignorando mayusculas. */
    Optional<User> findByEmailIgnoreCase(String email);

    /** BR-USER-001: unicidad de username. */
    boolean existsByUsername(String username);

    /** BR-USER-002: unicidad de email ignorando mayusculas. */
    boolean existsByEmailIgnoreCase(String email);
}
