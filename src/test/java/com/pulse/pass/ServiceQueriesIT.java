package com.pulse.pass;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.VenueRepository;

/**
 * Verifica contra PostgreSQL real los query methods agregados para la capa Service
 * (los unit tests de Service los mockean, asi que esta es su unica prueba real).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ServiceQueriesIT {

    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void venues_activeOnlyOrderedByName() {
        venueRepository.saveAndFlush(new Venue("SQ-V-B", "Zeta Hall", "Santa Marta", "x", 10, true));
        venueRepository.saveAndFlush(new Venue("SQ-V-A", "Alpha Hall", "Santa Marta", "x", 10, true));
        venueRepository.saveAndFlush(new Venue("SQ-V-C", "Inactive Hall", "Santa Marta", "x", 10, false));

        List<Venue> result = venueRepository.findByActiveTrueOrderByNameAsc();

        assertThat(result).extracting(Venue::getCode).containsSubsequence("SQ-V-A", "SQ-V-B");
        assertThat(result).extracting(Venue::getCode).doesNotContain("SQ-V-C");
    }

    @Test
    void events_existsByEventCode() {
        Venue venue = venueRepository.saveAndFlush(new Venue("SQ-V-E", "Hall", "Santa Marta", "x", 10, true));
        eventRepository.saveAndFlush(new Event("SQ-EVT-1", "Evt", "d", EventCategory.MUSIC,
                EventStatus.DRAFT, LocalDateTime.now().plusDays(10), 0, null, venue));

        assertThat(eventRepository.existsByEventCode("SQ-EVT-1")).isTrue();
        assertThat(eventRepository.existsByEventCode("SQ-EVT-X")).isFalse();
    }

    @Test
    void artists_caseInsensitiveLookupAndActiveOrdering() {
        artistRepository.saveAndFlush(new Artist("SQ Zulu", "CO", "Pop", true));
        artistRepository.saveAndFlush(new Artist("SQ Alpha", "CO", "Pop", true));
        artistRepository.saveAndFlush(new Artist("SQ Retired", "CO", "Pop", false));

        assertThat(artistRepository.findByStageNameIgnoreCase("sq alpha")).isPresent();
        assertThat(artistRepository.findByActiveTrueOrderByStageNameAsc())
                .extracting(Artist::getStageName)
                .containsSubsequence("SQ Alpha", "SQ Zulu")
                .doesNotContain("SQ Retired");
    }

    @Test
    void users_uniquenessChecksIgnoreCaseWhenRequired() {
        userRepository.saveAndFlush(new User("sq-andrea", "SQ.Andrea@Email.com", true));

        assertThat(userRepository.existsByUsername("sq-andrea")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("sq.andrea@email.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("nobody@email.com")).isFalse();
    }

    @Test
    void tickets_byUserEmailIgnoreCase_newestFirst() {
        Venue venue = venueRepository.saveAndFlush(new Venue("SQ-V-T", "Hall", "Santa Marta", "x", 10, true));
        Event event = eventRepository.saveAndFlush(new Event("SQ-EVT-T", "Evt", "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10), 0, null, venue));
        User user = userRepository.saveAndFlush(new User("sq-carlos", "SQ.Carlos@Email.com", true));
        LocalDateTime now = LocalDateTime.now();
        ticketRepository.saveAndFlush(new Ticket("SQ-T-OLD", TicketType.GENERAL, new BigDecimal("10.00"),
                TicketStatus.PAID, now.minusDays(2), user, event));
        ticketRepository.saveAndFlush(new Ticket("SQ-T-NEW", TicketType.VIP, new BigDecimal("25.00"),
                TicketStatus.PAID, now.minusDays(1), user, event));

        List<Ticket> result = ticketRepository.findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc("sq.carlos@email.com");

        assertThat(result).extracting(Ticket::getTicketCode).containsExactly("SQ-T-NEW", "SQ-T-OLD");
    }
}
