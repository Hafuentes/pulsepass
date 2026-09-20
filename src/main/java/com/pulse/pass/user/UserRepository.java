package com.pulse.pass.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** FR-USR-002: recuperar por username unico. */
    Optional<User> findByUsername(String username);

    /** Estrategia de consultas sec.14: buscar usuario por email ignorando mayusculas. */
    Optional<User> findByEmailIgnoreCase(String email);
}
