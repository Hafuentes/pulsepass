package com.pulse.pass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pulse.pass.domain.Role;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.dto.request.LoginRequest;
import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.AuthResponse;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRoleRepository;
import com.pulse.pass.security.JwtService;
import com.pulse.pass.security.SecurityPrincipal;

class AuthServiceTest {

    private final UserService userService = mock(UserService.class);
    private final UserCredentialRepository credentialRepository = mock(UserCredentialRepository.class);
    private final UserRoleRepository roleRepository = mock(UserRoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T15:00:00Z"), ZoneOffset.UTC);
    private final AuthService service = new AuthService(userService, credentialRepository, roleRepository,
            passwordEncoder, authenticationManager, jwtService, clock);

    private RegisterUserRequest registerRequest() {
        return new RegisterUserRequest("andrea", "andrea@mail.com", "Andrea", "Lopez", null, null,
                LocalDate.of(1995, 5, 20), "UnaFraseSeguraLarga2026!");
    }

    private UserResponse userResponse() {
        return new UserResponse(10L, "andrea", "andrea@mail.com", true, "Andrea", "Lopez", null, null,
                LocalDate.of(1995, 5, 20));
    }

    @Test
    void register_persistsHashedCredentialAndUserRoleOnly() {
        when(userService.register(any())).thenReturn(userResponse());
        when(passwordEncoder.encode("UnaFraseSeguraLarga2026!")).thenReturn("$2a$hashed");

        UserResponse result = service.register(registerRequest());

        assertThat(result.id()).isEqualTo(10L);
        ArgumentCaptor<UserCredential> credential = ArgumentCaptor.forClass(UserCredential.class);
        verify(credentialRepository).save(credential.capture());
        assertThat(credential.getValue().getUserId()).isEqualTo(10L);
        assertThat(credential.getValue().getPasswordHash()).isEqualTo("$2a$hashed").isNotEqualTo("UnaFraseSeguraLarga2026!");
        ArgumentCaptor<UserRole> role = ArgumentCaptor.forClass(UserRole.class);
        verify(roleRepository).save(role.capture());
        assertThat(role.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_userServiceFailure_createsNoCredentials() {
        when(userService.register(any())).thenThrow(new IllegalStateException("duplicate"));

        assertThatThrownBy(() -> service.register(registerRequest())).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(credentialRepository, roleRepository);
    }

    @Test
    void login_success_issuesTokenOnlyAfterAuthentication() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        SecurityPrincipal principal = new SecurityPrincipal(10L, "andrea", "andrea@mail.com", "h", true, authorities);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
        when(jwtService.generateToken(10L)).thenReturn("jwt");
        when(jwtService.getExpiresInSeconds()).thenReturn(900L);

        AuthResponse response = service.login(new LoginRequest("andrea", "pw"));

        assertThat(response.accessToken()).isEqualTo("jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    @Test
    void login_wrongPassword_throwsGenericBadCredentials_noToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        assertThatThrownBy(() -> service.login(new LoginRequest("andrea", "bad")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_inactiveUser_isIndistinguishableFromWrongPassword() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> service.login(new LoginRequest("andrea", "pw")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void me_returnsMinimalInformationWithoutSecrets() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityPrincipal principal = new SecurityPrincipal(10L, "andrea", "andrea@mail.com", "secret-hash", true, authorities);

        var me = service.me(principal);

        assertThat(me.id()).isEqualTo(10L);
        assertThat(me.roles()).containsExactly("ROLE_ADMIN", "ROLE_USER");
        assertThat(me.toString()).doesNotContain("secret-hash");
    }
}
