package com.pulse.pass.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.domain.Role;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;

/**
 * Semilla LOCAL y controlada de cuentas privilegiadas (PRD 9.2): los roles STAFF/ADMIN jamas se
 * asignan por HTTP. Solo se activa con el perfil "local-seed" y toma las contrasenas del entorno
 * (PULSEPASS_SEED_ADMIN_PASSWORD / PULSEPASS_SEED_STAFF_PASSWORD, minimo 12 caracteres).
 * Si una variable falta, esa cuenta no se crea. No usar en produccion.
 */
@Component
@Profile("local-seed")
public class LocalSecuritySeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSecuritySeedRunner.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String adminPassword;
    private final String staffPassword;

    public LocalSecuritySeedRunner(UserRepository userRepository,
                                   UserCredentialRepository credentialRepository,
                                   UserRoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder,
                                   Clock clock,
                                   @Value("${PULSEPASS_SEED_ADMIN_PASSWORD:}") String adminPassword,
                                   @Value("${PULSEPASS_SEED_STAFF_PASSWORD:}") String staffPassword) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.adminPassword = adminPassword;
        this.staffPassword = staffPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seed("admin", "admin@pulsepass.local", adminPassword, Set.of(Role.ADMIN, Role.USER));
        seed("laura.staff", "laura.staff@pulsepass.local", staffPassword, Set.of(Role.STAFF));
    }

    private void seed(String username, String email, String password, Set<Role> roles) {
        if (password == null || password.length() < 12) {
            log.warn("Seed account '{}' skipped: password env var missing or shorter than 12 characters.", username);
            return;
        }
        if (userRepository.existsByUsername(username)) {
            log.info("Seed account '{}' already exists; skipped.", username);
            return;
        }
        User user = userRepository.save(new User(username, email, true));
        credentialRepository.save(new UserCredential(user.getId(), passwordEncoder.encode(password),
                LocalDateTime.now(clock)));
        roles.forEach(role -> roleRepository.save(new UserRole(user.getId(), role)));
        log.info("Seed account '{}' created with roles {}.", username, roles);
    }
}
