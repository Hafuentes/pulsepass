package com.pulse.pass.config;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;

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
 * Procedimiento controlado para activar a UN usuario preexistente (anterior a la Fase 4) que quedo sin
 * credenciales tras la migracion V4 (PRD 9.2). Solo se ejecuta con el perfil "legacy-activation" y toma
 * usuario y contrasena del entorno (PULSEPASS_ACTIVATE_USERNAME / PULSEPASS_ACTIVATE_PASSWORD):
 * ninguna contrasena viaja en el codigo, en migraciones ni se comparte entre usuarios.
 *
 * Garantias: solo activa usuarios existentes SIN credencial (no sobrescribe hashes), valida la
 * contrasena (12 caracteres, 72 bytes BCrypt), nunca asigna STAFF/ADMIN y nunca registra la contrasena.
 * Limitacion del MVP: no existe aun "cambio de contrasena"; entregue la contrasena por un canal seguro.
 */
@Component
@Profile("legacy-activation")
public class LegacyUserActivationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyUserActivationRunner.class);

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String username;
    private final String password;

    public LegacyUserActivationRunner(UserRepository userRepository,
                                      UserCredentialRepository credentialRepository,
                                      UserRoleRepository roleRepository,
                                      PasswordEncoder passwordEncoder,
                                      Clock clock,
                                      @Value("${PULSEPASS_ACTIVATE_USERNAME:}") String username,
                                      @Value("${PULSEPASS_ACTIVATE_PASSWORD:}") String password) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (username == null || username.isBlank()) {
            log.warn("Legacy activation skipped: PULSEPASS_ACTIVATE_USERNAME is not set.");
            return;
        }
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            log.warn("Legacy activation skipped: PULSEPASS_ACTIVATE_PASSWORD must have at least 12 "
                    + "characters and at most 72 bytes.");
            return;
        }
        User user = userRepository.findByUsername(username.trim()).orElse(null);
        if (user == null) {
            log.warn("Legacy activation skipped: user not found.");
            return;
        }
        if (credentialRepository.existsById(user.getId())) {
            log.warn("Legacy activation skipped: userId={} already has credentials.", user.getId());
            return;
        }

        credentialRepository.save(new UserCredential(
                user.getId(), passwordEncoder.encode(password), LocalDateTime.now(clock)));
        if (roleRepository.findByUserId(user.getId()).isEmpty()) {
            roleRepository.save(new UserRole(user.getId(), Role.USER)); // nunca STAFF/ADMIN
        }
        log.info("Legacy account userId={} activated with a BCrypt credential.", user.getId());
    }
}
