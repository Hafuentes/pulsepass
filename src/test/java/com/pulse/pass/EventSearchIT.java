package com.pulse.pass;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.artist.Artist;
import com.pulse.pass.artist.ArtistRepository;
import com.pulse.pass.event.Event;
import com.pulse.pass.event.EventCategory;
import com.pulse.pass.event.EventRepository;
import com.pulse.pass.event.EventStatus;
import com.pulse.pass.venue.Venue;
import com.pulse.pass.venue.VenueRepository;

/**
 * Cubre FR-ART-003/004, FR-SRC-001..003 y AC-003, AC-006, AC-007
 * (relacion N:M Event-Artist y consultas JPQL de descubrimiento).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventSearchIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Test
    void unEventoPuedeAsociarVariosArtistasSinDuplicarElParEventoArtista() {
        Venue venue = venueRepository.saveAndFlush(
                new Venue("VEN-SRC-01", "Marina Convention Center", "Santa Marta", "Calle 1", 5000, true));
        Artist a1 = artistRepository.saveAndFlush(new Artist("Solar Beat Test", "Colombia", "Electronic", true));
        Artist a2 = artistRepository.saveAndFlush(new Artist("Neon Waves Test", "Mexico", "Synthwave", true));
        Artist a3 = artistRepository.saveAndFlush(new Artist("Caribbean Sound Test", "Colombia", "Reggae", true));

        Event event = new Event("CMF-SRC-01", "Caribbean Music Fest", "desc", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(15), 0, null, venue);
        event.addArtist(a1);
        event.addArtist(a2);
        event.addArtist(a3);
        event.addArtist(a1); // intento de duplicar el mismo par evento-artista

        Event guardado = eventRepository.saveAndFlush(event);

        assertThat(guardado.getArtists()).hasSize(3);
        assertThat(guardado.getArtists()).extracting(Artist::getStageName)
                .containsExactlyInAnyOrder("Solar Beat Test", "Neon Waves Test", "Caribbean Sound Test");
    }

    @Test
    void buscarEventosPorArtistaNoDevuelveDuplicados() {
        Venue venue = venueRepository.saveAndFlush(
                new Venue("VEN-SRC-02", "Estadio Norte", "Bogota", "Cra 1", 20000, true));
        Artist solarBeat = artistRepository.saveAndFlush(new Artist("Solar Beat SRC2", "Colombia", "Electronic", true));

        Event e1 = new Event("SRC2-E1", "Concierto 1", "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(1), 0, null, venue);
        e1.addArtist(solarBeat);
        Event e2 = new Event("SRC2-E2", "Concierto 2", "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(2), 0, null, venue);
        e2.addArtist(solarBeat);
        eventRepository.saveAndFlush(e1);
        eventRepository.saveAndFlush(e2);

        List<Event> eventos = eventRepository.findEventsByArtistStageName("Solar Beat SRC2");

        assertThat(eventos).extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("SRC2-E1", "SRC2-E2");
    }

    @Test
    void filtraEventosPorCiudadYArtista() {
        Venue venueSantaMarta = venueRepository.saveAndFlush(
                new Venue("VEN-SRC-03A", "Marina Convention Center", "Santa Marta", "Calle 1", 5000, true));
        Venue venueBogota = venueRepository.saveAndFlush(
                new Venue("VEN-SRC-03B", "Movistar Arena", "Bogota", "Cra 1", 15000, true));
        Artist artist = artistRepository.saveAndFlush(new Artist("Ocean Drive SRC3", "USA", "Pop", true));

        Event enSantaMarta = new Event("SRC3-SM", "Evento SM", "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(5), 0, null, venueSantaMarta);
        enSantaMarta.addArtist(artist);
        Event enBogota = new Event("SRC3-BOG", "Evento BOG", "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(6), 0, null, venueBogota);
        enBogota.addArtist(artist);
        eventRepository.saveAndFlush(enSantaMarta);
        eventRepository.saveAndFlush(enBogota);

        List<Event> resultado = eventRepository.findEventsByCityAndArtistStageName("Santa Marta", "Ocean Drive SRC3");

        assertThat(resultado).extracting(Event::getEventCode).containsExactly("SRC3-SM");
    }

    @Test
    void eventosRecomendadosSonCaseInsensitiveSinDuplicadosYOrdenadosPorFecha() {
        Venue venue = venueRepository.saveAndFlush(
                new Venue("VEN-SRC-04", "Marina Convention Center", "Santa Marta", "Calle 1", 5000, true));
        Artist digitalPulse = artistRepository.saveAndFlush(new Artist("Digital Pulse SRC4", "Espana", "Techno", true));

        LocalDateTime ahora = LocalDateTime.now();

        Event futuroTemprano = new Event("SRC4-1", "Festival Temprano", "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, ahora.plusDays(3), 0, null, venue);
        futuroTemprano.addArtist(digitalPulse);
        Event futuroTardio = new Event("SRC4-2", "Festival Tardio", "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, ahora.plusDays(20), 0, null, venue);
        futuroTardio.addArtist(digitalPulse);
        Event borrador = new Event("SRC4-3", "Festival Borrador", "d", EventCategory.MUSIC,
                EventStatus.DRAFT, ahora.plusDays(5), 0, null, venue);
        borrador.addArtist(digitalPulse);
        Event pasado = new Event("SRC4-4", "Festival Pasado", "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, ahora.minusDays(1), 0, null, venue);
        pasado.addArtist(digitalPulse);

        eventRepository.saveAndFlush(futuroTardio);
        eventRepository.saveAndFlush(futuroTemprano);
        eventRepository.saveAndFlush(borrador);
        eventRepository.saveAndFlush(pasado);

        List<Event> recomendados = eventRepository.findRecommendedEvents(
                "Santa Marta", "digital pulse src4", ahora);

        assertThat(recomendados).extracting(Event::getEventCode)
                .containsExactly("SRC4-1", "SRC4-2");
    }
}
