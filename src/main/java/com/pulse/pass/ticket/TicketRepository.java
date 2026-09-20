package com.pulse.pass.ticket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** FR-TKT-002: recuperar por ticketCode unico. */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /** FR-TKT-006: tickets de un usuario navegando Ticket -> User -> email. */
    List<Ticket> findByUser_Email(String email);

    /** FR-TKT-006: tickets de un usuario filtrados opcionalmente por estado. */
    List<Ticket> findByUser_EmailAndStatus(String email, TicketStatus status);

    /** FR-TKT-007: tickets PAID (o de cualquier estado) de un evento por eventCode. */
    List<Ticket> findByEvent_EventCodeAndStatus(String eventCode, TicketStatus status);

    /** FR-TKT-008: conteo de tickets PAID de un evento (JPQL con COUNT). */
    @Query("""
            SELECT COUNT(t) FROM Ticket t
            WHERE t.event.eventCode = :eventCode
              AND t.status = com.pulse.pass.ticket.TicketStatus.PAID
            """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    /** FR-SRC-004: tickets cuyo evento es posterior a una fecha, ordenados cronologicamente. */
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.event.eventDate > :afterDate
            ORDER BY t.event.eventDate ASC
            """)
    List<Ticket> findTicketsForFutureEvents(@Param("afterDate") LocalDateTime afterDate);
}
