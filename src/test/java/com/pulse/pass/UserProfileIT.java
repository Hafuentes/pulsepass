package com.pulse.pass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.user.User;
import com.pulse.pass.user.UserProfile;
import com.pulse.pass.user.UserProfileRepository;
import com.pulse.pass.user.UserRepository;

/**
 * Cubre FR-USR-001..004, QT-004 (relacion User 1:1 UserProfile) y AC-004
 * (la FK unica de user_profiles.user_id impide un segundo perfil para el mismo usuario).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserProfileIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void unUsuarioValidoSePersisteYSeRecupera() {
        User user = userRepository.saveAndFlush(new User("andrea.gomez", "andrea@pulsepass.com", true));

        assertThat(user.getId()).isNotNull();
        assertThat(userRepository.findByUsername("andrea.gomez")).isPresent();
    }

    @Test
    void usernameDebeSerUnico() {
        userRepository.saveAndFlush(
                new User("carlos.ruiz", "carlos@pulsepass.com", true));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("carlos.ruiz", "otro@pulsepass.com", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailDebeSerUnico() {
        userRepository.saveAndFlush(
                new User("carlos.ruiz.email", "carlos@pulsepass.com", true));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("otro.usuario", "carlos@pulsepass.com", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cadaUsuarioPuedePoseerUnUnicoUserProfile() {
        User user = userRepository.saveAndFlush(new User("laura.diaz", "laura@pulsepass.com", true));
        UserProfile perfil = new UserProfile("Laura", "Diaz", "3001234567", "Medellin",
                LocalDate.of(1995, 4, 12), user);

        UserProfile guardado = userProfileRepository.saveAndFlush(perfil);

        assertThat(guardado.getUser().getUsername()).isEqualTo("laura.diaz");
        assertThat(guardado.getFirstName()).isEqualTo("Laura");
    }

    @Test
    void laBaseDeDatosImpideUnSegundoPerfilParaElMismoUsuario() {
        User user = userRepository.saveAndFlush(new User("miguel.torres", "miguel@pulsepass.com", true));
        userProfileRepository.saveAndFlush(
                new UserProfile("Miguel", "Torres", "3005551234", "Cali", LocalDate.of(1990, 1, 1), user));

        UserProfile segundoPerfil = new UserProfile("Miguel", "Torres Segundo", "3009998888",
                "Cali", LocalDate.of(1990, 1, 1), user);

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(segundoPerfil))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void buscarUsuarioPorEmailIgnorandoMayusculas() {
        userRepository.saveAndFlush(new User("ana.paola", "Ana.Paola@PulsePass.com", true));

        Optional<User> encontrado = userRepository.findByEmailIgnoreCase("ana.paola@pulsepass.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsername()).isEqualTo("ana.paola");
    }
}
