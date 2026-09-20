package com.pulse.pass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.event.Event;
import com.pulse.pass.event.EventCategory;
import com.pulse.pass.event.EventRepository;
import com.pulse.pass.event.EventStatus;
import com.pulse.pass.ticket.Ticket;
import com.pulse.pass.ticket.TicketRepository;
import com.pulse.pass.ticket.TicketStatus;
import com.pulse.pass.ticket.TicketType;
import com.pulse.pass.user.User;
import com.pulse.pass.user.UserRepository;
import com.pulse.pass.venue.Venue;
import com.pulse.pass.venue.VenueRepository;

/**
 * Cubre FR-TKT-001..008, FR-SRC-004, QT-006 (Ticket -> User, Ticket -> Event)
 * y QT-008/QT-009 (JPQL con COUNT y restriccion UNIQUE via saveAndFlush).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketRepositoryIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Event nuevoEvento(String eventCode, LocalDateTime fecha) {
        Venue venue = venueRepository.saveAndFlush(
                new Venue("VEN-TKT-" + eventCode, "Marina Convention Center", "Santa Marta", "Calle 1", 5000, true));
        return eventRepository.saveAndFlush(new Event(eventCode, "Evento " + eventCode, "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED, fecha, 0, null, venue));
    }

    private User nuevoUsuario(String username, String email) {
        return userRepository.saveAndFlush(new User(username, email, true));
    }

    @Test
    void unTicketQuedaAsociadoAUnUsuarioYAUnEvento() {
        Event event = nuevoEvento("TKT-CMF", LocalDateTime.now().plusDays(30));
        User user = nuevoUsuario("andrea.tkt", "andrea.tkt@pulsepass.com");

        Ticket ticket = new Ticket("TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, LocalDateTime.now(), user, event);

        Ticket guardado = ticketRepository.saveAndFlush(ticket);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getUser().getUsername()).isEqualTo("andrea.tkt");
        assertThat(guardado.getEvent().getEventCode()).isEqualTo("TKT-CMF");
    }

    @Test
    void ticketCodeDebeSerUnico() {
        Event event = nuevoEvento("TKT-DUP", LocalDateTime.now().plusDays(10));
        User user = nuevoUsuario("carlos.tkt", "carlos.tkt@pulsepass.com");
        ticketRepository.saveAndFlush(new Ticket("TCK-DUP-01", TicketType.GENERAL,
                new BigDecimal("120000.00"), TicketStatus.PAID, LocalDateTime.now(), user, event));

        Ticket duplicado = new Ticket("TCK-DUP-01", TicketType.GENERAL,
                new BigDecimal("120000.00"), TicketStatus.RESERVED, LocalDateTime.now(), user, event);

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elPrecioNoPuedeSerNegativo() {
        Event event = nuevoEvento("TKT-NEG", LocalDateTime.now().plusDays(10));
        User user = nuevoUsuario("laura.tkt", "laura.tkt@pulsepass.com");

        Ticket precioNegativo = new Ticket("TCK-NEG-01", TicketType.GENERAL,
                new BigDecimal("-10.00"), TicketStatus.RESERVED, LocalDateTime.now(), user, event);

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(precioNegativo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seRecuperanLosTicketsDeUnUsuarioPorEmailYEstado() {
        Event event = nuevoEvento("TKT-USR", LocalDateTime.now().plusDays(10));
        User user = nuevoUsuario("miguel.tkt", "miguel.tkt@pulsepass.com");

        ticketRepository.saveAndFlush(new Ticket("TCK-USR-01", TicketType.VIP,
                new BigDecimal("250000.00"), TicketStatus.PAID, LocalDateTime.now(), user, event));
        ticketRepository.saveAndFlush(new Ticket("TCK-USR-02", TicketType.VIP,
                new BigDecimal("250000.00"), TicketStatus.CANCELLED, LocalDateTime.now(), user, event));

        List<Ticket> todos = ticketRepository.findByUser_Email("miguel.tkt@pulsepass.com");
        List<Ticket> pagados = ticketRepository.findByUser_EmailAndStatus(
                "miguel.tkt@pulsepass.com", TicketStatus.PAID);

        assertThat(todos).hasSize(2);
        assertThat(pagados).extracting(Ticket::getTicketCode).containsExactly("TCK-USR-01");
    }

    @Test
    void seRecuperanSoloLosTicketsPagadosDeUnEventoYSeCuentanCorrectamente() {
        Event event = nuevoEvento("TKT-SALES", LocalDateTime.now().plusDays(10));
        User andrea = nuevoUsuario("andrea.sales", "andrea.sales@pulsepass.com");
        User carlos = nuevoUsuario("carlos.sales", "carlos.sales@pulsepass.com");
        User laura = nuevoUsuario("laura.sales", "laura.sales@pulsepass.com");
        User miguelUsr = nuevoUsuario("miguel.sales", "miguel.sales@pulsepass.com");

        ticketRepository.saveAndFlush(new Ticket("TCK-SALES-01", TicketType.VIP,
                new BigDecimal("250000.00"), TicketStatus.PAID, LocalDateTime.now(), andrea, event));
        ticketRepository.saveAndFlush(new Ticket("TCK-SALES-02", TicketType.GENERAL,
                new BigDecimal("120000.00"), TicketStatus.PAID, LocalDateTime.now(), carlos, event));
        ticketRepository.saveAndFlush(new Ticket("TCK-SALES-03", TicketType.GENERAL,
                new BigDecimal("120000.00"), TicketStatus.RESERVED, LocalDateTime.now(), laura, event));
        ticketRepository.saveAndFlush(new Ticket("TCK-SALES-04", TicketType.VIP,
                new BigDecimal("250000.00"), TicketStatus.CANCELLED, LocalDateTime.now(), miguelUsr, event));

        List<Ticket> pagados = ticketRepository.findByEvent_EventCodeAndStatus("TKT-SALES", TicketStatus.PAID);
        long conteo = ticketRepository.countPaidTicketsByEventCode("TKT-SALES");

        assertThat(pagados).extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder("TCK-SALES-01", "TCK-SALES-02");
        assertThat(conteo).isEqualTo(2L);
    }

    @Test
    void seRecuperanLosTicketsDeEventosFuturosOrdenadosCronologicamente() {
        LocalDateTime ahora = LocalDateTime.now();
        Event futuroTardio = nuevoEvento("TKT-FUT-2", ahora.plusDays(20));
        Event futuroTemprano = nuevoEvento("TKT-FUT-1", ahora.plusDays(3));
        Event pasado = nuevoEvento("TKT-PAST", ahora.minusDays(3));
        User user = nuevoUsuario("sofia.fut", "sofia.fut@pulsepass.com");

        ticketRepository.saveAndFlush(new Ticket("TCK-FUT-01", TicketType.GENERAL,
                new BigDecimal("100000.00"), TicketStatus.PAID, LocalDateTime.now(), user, futuroTardio));
        ticketRepository.saveAndFlush(new Ticket("TCK-FUT-02", TicketType.GENERAL,
                new BigDecimal("100000.00"), TicketStatus.PAID, LocalDateTime.now(), user, futuroTemprano));
        ticketRepository.saveAndFlush(new Ticket("TCK-FUT-03", TicketType.GENERAL,
                new BigDecimal("100000.00"), TicketStatus.PAID, LocalDateTime.now(), user, pasado));

        List<Ticket> futuros = ticketRepository.findTicketsForFutureEvents(ahora);

        assertThat(futuros).extracting(Ticket::getTicketCode)
                .containsExactly("TCK-FUT-02", "TCK-FUT-01");
    }
}
