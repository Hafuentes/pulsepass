package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.pulse.pass.domain.Role;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserCredentialRepository credentialRepository = mock(UserCredentialRepository.class);
    private final UserRoleRepository roleRepository = mock(UserRoleRepository.class);
    private final CustomUserDetailsService service =
            new CustomUserDetailsService(userRepository, credentialRepository, roleRepository);

    private User user(long id, boolean active) throws Exception {
        User user = new User("andrea", "andrea@mail.com", active);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    private void credentialsAndRoles(long id, Role... roles) {
        when(credentialRepository.findById(id))
                .thenReturn(Optional.of(new UserCredential(id, "$2a$hash", LocalDateTime.now())));
        when(roleRepository.findByUserId(id))
                .thenReturn(java.util.Arrays.stream(roles).map(r -> new UserRole(id, r)).toList());
    }

    @Test
    void loadByUsername_buildsPrincipalWithRolesAndState() throws Exception {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(5L, true)));
        credentialsAndRoles(5L, Role.USER, Role.STAFF);

        SecurityPrincipal principal = (SecurityPrincipal) service.loadUserByUsername("andrea");

        assertThat(principal.getUserId()).isEqualTo(5L);
        assertThat(principal.getPassword()).isEqualTo("$2a$hash");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
        assertThat(principal.toString()).doesNotContain("hash");
    }

    @Test
    void loadByEmail_fallsBackToEmailLookup() throws Exception {
        when(userRepository.findByUsername("andrea@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(user(5L, true)));
        credentialsAndRoles(5L, Role.USER);

        assertThat(service.loadUserByUsername("andrea@mail.com")).isNotNull();
    }

    @Test
    void unknownUser_throwsUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void legacyUserWithoutCredentials_cannotAuthenticate() throws Exception {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(5L, true)));
        when(credentialRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("andrea")).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void inactiveUser_isDisabled() throws Exception {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, false)));
        credentialsAndRoles(5L, Role.USER);

        assertThat(service.loadPrincipalById(5L).isEnabled()).isFalse();
    }

    @Test
    void rolesAreReloadedOnEachCall() throws Exception {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, true)));
        credentialsAndRoles(5L, Role.ADMIN);
        assertThat(service.loadPrincipalById(5L).hasRole("ADMIN")).isTrue();

        when(roleRepository.findByUserId(5L)).thenReturn(List.of(new UserRole(5L, Role.USER)));
        assertThat(service.loadPrincipalById(5L).hasRole("ADMIN")).isFalse();
    }
}
