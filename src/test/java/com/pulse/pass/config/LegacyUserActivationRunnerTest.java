package com.pulse.pass.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pulse.pass.domain.Role;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;

/** PRD 9.2: activacion controlada de usuarios legados, sin contrasenas por defecto ni roles privilegiados. */
class LegacyUserActivationRunnerTest {

    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserCredentialRepository credentialRepository = mock(UserCredentialRepository.class);
    private final UserRoleRepository roleRepository = mock(UserRoleRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC);

    private LegacyUserActivationRunner runner(String username, String password) {
        return new LegacyUserActivationRunner(userRepository, credentialRepository, roleRepository, encoder,
                clock, username, password);
    }

    private User legacyUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        when(userRepository.findByUsername("old-user")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void activatesLegacyUserWithBcryptCredential_andOnlyUserRole() {
        legacyUser();
        when(credentialRepository.existsById(5L)).thenReturn(false);
        when(roleRepository.findByUserId(5L)).thenReturn(List.of());
        when(encoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");

        runner("old-user", PASSWORD).run();

        ArgumentCaptor<UserCredential> credential = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).save(credential.capture());
        assertThat(credential.getValue().getUserId()).isEqualTo(5L);
        assertThat(credential.getValue().getPasswordHash()).isEqualTo("$2a$10$hashed").isNotEqualTo(PASSWORD);

        ArgumentCaptor<UserRole> role = ArgumentCaptor.forClass(UserRole.class);
        verify(roleRepository).save(role.capture());
        assertThat(role.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void doesNotDuplicateExistingRoles() {
        legacyUser();
        when(credentialRepository.existsById(5L)).thenReturn(false);
        when(roleRepository.findByUserId(5L)).thenReturn(List.of(new UserRole(5L, Role.USER)));
        when(encoder.encode(PASSWORD)).thenReturn("$2a$10$hashed");

        runner("old-user", PASSWORD).run();

        verify(credentialRepository).save(any(UserCredential.class));
        verify(roleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void neverOverwritesExistingCredentials() {
        legacyUser();
        when(credentialRepository.existsById(5L)).thenReturn(true);

        runner("old-user", PASSWORD).run();

        verify(credentialRepository, never()).save(any(UserCredential.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void skipsWhenNoUsernameIsProvided() {
        runner("", PASSWORD).run();

        verifyNoInteractions(userRepository, credentialRepository, roleRepository, encoder);
    }

    @Test
    void skipsWeakOrOversizedPasswords() {
        runner("old-user", "corta").run();
        runner("old-user", "ñ".repeat(40)).run(); // 80 bytes UTF-8 > limite BCrypt

        verifyNoInteractions(userRepository, credentialRepository, roleRepository, encoder);
    }

    @Test
    void skipsUnknownUsers() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        runner("ghost", PASSWORD).run();

        verify(credentialRepository, never()).save(any(UserCredential.class));
        verify(roleRepository, never()).save(any(UserRole.class));
    }
}
