package com.pulse.pass.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.domain.Role;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.dto.request.LoginRequest;
import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.AuthResponse;
import com.pulse.pass.dto.response.AuthenticatedUserResponse;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRoleRepository;
import com.pulse.pass.security.JwtService;
import com.pulse.pass.security.SecurityPrincipal;

/**
 * Orquesta registro (usuario + credencial BCrypt + ROLE_USER, atomico) y login (JWT).
 * No reemplaza a UserService: delega en el para las reglas de negocio del alta.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthService(UserService userService,
                       UserCredentialRepository credentialRepository,
                       UserRoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       Clock clock) {
        this.userService = userService;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /**
     * FR-SECAUTH-001 / NFR-SEC004: una unica transaccion; si falla la credencial o el rol,
     * tambien se revierte el usuario. El rol inicial es siempre USER (NFR-SEC009).
     */
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        UserResponse user = userService.register(request);
        credentialRepository.save(new UserCredential(
                user.id(), passwordEncoder.encode(request.password()), LocalDateTime.now(clock)));
        roleRepository.save(new UserRole(user.id(), Role.USER));
        return user;
    }

    /** FR-SECAUTH-002/003/004: cualquier fallo produce el mismo 401 generico. */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.identifier(), request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Login failed for identifier='{}'", sanitizeForLog(request.identifier()));
            throw new BadCredentialsException("Invalid credentials");
        }
        SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();
        log.info("Login succeeded for userId={}", principal.getUserId());
        return new AuthResponse(jwtService.generateToken(principal.getUserId()), "Bearer",
                jwtService.getExpiresInSeconds());
    }

    /** Evita inyeccion de saltos de linea/control en logs con texto controlado por el cliente. */
    private static String sanitizeForLog(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replaceAll("\\p{Cntrl}", "_");
        return clean.length() > 80 ? clean.substring(0, 80) + "..." : clean;
    }

    public AuthenticatedUserResponse me(SecurityPrincipal principal) {
        return new AuthenticatedUserResponse(
                principal.getUserId(),
                principal.getUsername(),
                principal.getEmail(),
                principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
    }
}
