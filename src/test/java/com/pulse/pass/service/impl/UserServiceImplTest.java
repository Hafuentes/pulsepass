package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserProfile;
import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.UserMapper;
import com.pulse.pass.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new UserServiceImpl(userRepository, userMapper, clock);
    }

    private RegisterUserRequest request(LocalDate birthDate) {
        return new RegisterUserRequest("andrea", "andrea@email.com", "Andrea", "Perez",
                "3001112233", "Santa Marta", birthDate);
    }

    private UserResponse response() {
        return new UserResponse(1L, "andrea", "andrea@email.com", true, "Andrea", "Perez",
                "3001112233", "Santa Marta", LocalDate.of(2001, 1, 1));
    }

    @Test
    void register_validUser_savesActiveUserWithProfile() { // TEST-USER-001
        // ARRANGE
        UserResponse expected = response();
        when(userRepository.save(any(User.class))).then(returnsFirstArg());
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        // ACT
        UserResponse result = service.register(request(LocalDate.of(2001, 1, 1)));

        // ASSERT
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getUsername()).isEqualTo("andrea");
        UserProfile profile = saved.getProfile();
        assertThat(profile).isNotNull();
        assertThat(profile.getUser()).isSameAs(saved);
        assertThat(profile.getFirstName()).isEqualTo("Andrea");
        assertThat(profile.getBirthDate()).isEqualTo(LocalDate.of(2001, 1, 1));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void register_duplicatedUsername_throwsDuplicateAndNeverSaves() { // TEST-USER-002
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> service.register(request(LocalDate.of(2001, 1, 1))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_duplicatedEmailIgnoringCase_throwsDuplicateAndNeverSaves() { // TEST-USER-003
        // ARRANGE
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> service.register(request(LocalDate.of(2001, 1, 1))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_futureBirthDate_throwsBusinessRuleAndNeverSaves() { // TEST-USER-004
        // ACT + ASSERT
        assertThatThrownBy(() -> service.register(request(LocalDate.of(2026, 10, 5))))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByEmail_existing_returnsDto() {
        // ARRANGE
        User user = new User("andrea", "andrea@email.com", true);
        when(userRepository.findByEmailIgnoreCase("ANDREA@email.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response());

        // ACT + ASSERT
        assertThat(service.findByEmail("ANDREA@email.com")).isEqualTo(response());
    }

    @Test
    void findByEmail_missing_throwsResourceNotFound() {
        when(userRepository.findByEmailIgnoreCase("x@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByEmail("x@email.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByUsername_existing_returnsDto() {
        User user = new User("andrea", "andrea@email.com", true);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response());

        assertThat(service.findByUsername("andrea")).isEqualTo(response());
    }

    @Test
    void findByUsername_missing_throwsResourceNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
