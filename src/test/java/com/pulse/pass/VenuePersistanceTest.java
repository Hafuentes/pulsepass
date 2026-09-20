package com.pulse.pass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.venue.Venue;
import com.pulse.pass.venue.VenueRepository;

/**
 * Cubre FR-VEN-001..004 y AC-001, contra PostgreSQL real via Testcontainers (NFR-005).
 * Cada test corre en su propia transaccion que se revierte al finalizar, para no
 * interferir con los demas tests de integracion que comparten el mismo contenedor.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class VenuePersistanceTest {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void unVenueValidoSePersisteYSeRecuperaPorIdYCodigo() {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta",
                "Calle 1 # 2-3", 5000, true);

        Venue guardado = venueRepository.saveAndFlush(venue);

        assertThat(guardado.getId()).isNotNull();
        assertThat(venueRepository.findById(guardado.getId())).isPresent();

        Optional<Venue> porCodigo = venueRepository.findByCode("VEN-SMR-01");
        assertThat(porCodigo).isPresent();
        assertThat(porCodigo.get().getCapacity()).isGreaterThan(0);
        assertThat(porCodigo.get().getCity()).isEqualTo("Santa Marta");
    }

    @Test
    void noPuedenExistirDosVenuesConElMismoCodigo() {
        venueRepository.saveAndFlush(
                new Venue("VEN-DUP-01", "Coliseo Norte", "Bogota", "Cra 1", 2000, true));

        Venue duplicado = new Venue("VEN-DUP-01", "Otro Recinto", "Medellin", "Cra 2", 1500, true);

        assertThatThrownBy(() -> venueRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void laCapacidadDebeSerMayorQueCero() {
        Venue capacidadInvalida = new Venue("VEN-CAP-01", "Auditorio Test", "Cali", "Av 5", 0, true);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(capacidadInvalida));
    }
}
