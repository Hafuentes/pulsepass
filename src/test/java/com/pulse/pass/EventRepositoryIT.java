package com.pulse.pass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.event.Event;
import com.pulse.pass.event.EventCategory;
import com.pulse.pass.event.EventRepository;
import com.pulse.pass.event.EventStatus;
import com.pulse.pass.venue.Venue;
import com.pulse.pass.venue.VenueRepository;

/**
 * Cubre FR-EVT-001..006, QT-002 (Hibernate valida el esquema al levantar el contexto),
 * QT-003 (relacion Venue 1:N Event) y QT-007 (Query Methods simples y con navegacion).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventRepositoryIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Venue nuevoVenue(String code) {
        return venueRepository.saveAndFlush(
                new Venue(code, "Marina Convention Center", "Santa Marta", "Calle 1", 5000, true));
    }

    @Test
    void unEventoValidoQuedaAsociadoAUnVenueExistente() {
        Venue venue = nuevoVenue("VEN-EVT-01");
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026", "Festival de musica",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30),
                0, null, venue);

        Event guardado = eventRepository.saveAndFlush(event);

        Optional<Event> recuperado = eventRepository.findByEventCode("CMF-2026");
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getVenue().getCode()).isEqualTo("VEN-EVT-01");
        assertThat(guardado.getId()).isNotNull();
    }

    @Test
    void eventCodeDebeSerUnico() {
        Venue venue = nuevoVenue("VEN-EVT-02");
        eventRepository.saveAndFlush(new Event("DUP-2026", "Evento A", "desc",
                EventCategory.CULTURE, EventStatus.DRAFT, LocalDateTime.now().plusDays(10), 0, null, venue));

        Event duplicado = new Event("DUP-2026", "Evento B", "otra desc",
                EventCategory.SPORTS, EventStatus.DRAFT, LocalDateTime.now().plusDays(20), 0, null, venue);

        assertThatThrownBy(() -> eventRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test
    void postgresRechazaCategoriaDeEventoFueraDelCatalogo() {
        Venue venue = nuevoVenue("VEN-EVT-CATEGORY");
        Event event = eventRepository.saveAndFlush(new Event(
                "CAT-2026",
                "Evento categoria",
                "d",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.now().plusDays(1),
                0,
                null,
                venue
        ));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE events SET category = 'INVALID_CATEGORY' WHERE id = ?",
                event.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void postgresRechazaEstadoDeEventoFueraDelCatalogo() {
        Venue venue = nuevoVenue("VEN-EVT-STATUS");
        Event event = eventRepository.saveAndFlush(new Event(
                "STATUS-2026",
                "Evento estado",
                "d",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.now().plusDays(1),
                0,
                null,
                venue
        ));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE events SET status = 'INVALID_STATUS' WHERE id = ?",
                event.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soloSeRecuperanEventosPublicadosOrdenadosPorFechaAscendente() {
        Venue venue = nuevoVenue("VEN-EVT-03");
        LocalDateTime base = LocalDateTime.now().plusDays(5);

        eventRepository.saveAndFlush(new Event("PUB-B", "Publicado tarde", "d",
                EventCategory.MUSIC, EventStatus.PUBLISHED, base.plusDays(10), 0, null, venue));
        eventRepository.saveAndFlush(new Event("PUB-A", "Publicado temprano", "d",
                EventCategory.MUSIC, EventStatus.PUBLISHED, base, 0, null, venue));
        eventRepository.saveAndFlush(new Event("DRAFT-1", "Borrador", "d",
                EventCategory.MUSIC, EventStatus.DRAFT, base.plusDays(1), 0, null, venue));
        eventRepository.saveAndFlush(new Event("CANC-1", "Cancelado", "d",
                EventCategory.MUSIC, EventStatus.CANCELLED, base.plusDays(2), 0, null, venue));

        List<Event> publicados = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(publicados).extracting(Event::getEventCode).containsExactly("PUB-A", "PUB-B");
    }

    @Test
    void seRecuperanSoloLosEventosDelVenueSolicitado() {
        Venue venueA = nuevoVenue("VEN-EVT-04A");
        Venue venueB = nuevoVenue("VEN-EVT-04B");

        eventRepository.saveAndFlush(new Event("EVT-A1", "Evento A1", "d",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(1), 0, null, venueA));
        eventRepository.saveAndFlush(new Event("EVT-A2", "Evento A2", "d",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2), 0, null, venueA));
        eventRepository.saveAndFlush(new Event("EVT-B1", "Evento B1", "d",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(3), 0, null, venueB));

        List<Event> eventosDeA = eventRepository.findByVenue_CodeOrderByEventDateAsc("VEN-EVT-04A");

        assertThat(eventosDeA).extracting(Event::getEventCode).containsExactly("EVT-A1", "EVT-A2");
    }

    @Test
    void unEventoHibridoPuedeTenerStreamingUrlNulaOAsignada() {
        Venue venue = nuevoVenue("VEN-EVT-05");

        Event sinStreaming = eventRepository.saveAndFlush(new Event("HYB-1", "Evento presencial", "d",
                EventCategory.TECHNOLOGY, EventStatus.DRAFT, LocalDateTime.now().plusDays(1), 0, null, venue));
        Event conStreaming = eventRepository.saveAndFlush(new Event("HYB-2", "Evento hibrido", "d",
                EventCategory.TECHNOLOGY, EventStatus.DRAFT, LocalDateTime.now().plusDays(2),
                0, "https://stream.pulsepass.com/hyb-2", venue));

        assertThat(sinStreaming.getStreamingUrl()).isNull();
        assertThat(eventRepository.findByEventCode("HYB-2").orElseThrow().getStreamingUrl())
                .isEqualTo("https://stream.pulsepass.com/hyb-2");
    }
}
