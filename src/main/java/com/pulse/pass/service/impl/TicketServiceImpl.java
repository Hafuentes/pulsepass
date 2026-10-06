package com.pulse.pass.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserProfile;
import com.pulse.pass.dto.request.PurchaseTicketRequest;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.TicketMapper;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.service.TicketPriceCalculator;
import com.pulse.pass.service.TicketService;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;
    private final TicketPriceCalculator priceCalculator;
    private final Clock clock;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             UserRepository userRepository,
                             EventRepository eventRepository,
                             TicketMapper ticketMapper,
                             TicketPriceCalculator priceCalculator,
                             Clock clock) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.priceCalculator = priceCalculator;
        this.clock = clock;
    }

    /**
     * BR-TICKET-001..009. Operacion atomica: si cualquier paso falla se hace rollback,
     * incluido el cambio a SOLD_OUT (BR-TICKET-008).
     */
    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));
        if (!user.isActive()) {
            throw new BusinessRuleException("User is not active: " + user.getEmail());
        }

        Event event = getEvent(request.eventCode());
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events. Event %s is %s."
                            .formatted(event.getEventCode(), event.getStatus()));
        }
        if (!event.getEventDate().isAfter(now())) {
            throw new BusinessRuleException("Event has already taken place: " + event.getEventCode());
        }

        validateMinimumAge(user, event);

        int capacity = event.getVenue().getCapacity();
        long paidTickets = ticketRepository.countPaidTicketsByEventCode(event.getEventCode());
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no capacity left: " + event.getEventCode());
        }

        Ticket ticket = new Ticket(
                generateTicketCode(),
                request.type(),
                priceCalculator.calculate(request.type()),
                TicketStatus.PAID,
                now(),
                user,
                event);
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: la compra que completa el aforo marca el evento como SOLD_OUT.
        if (paidTickets + 1 >= capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(getTicket(ticketCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        Event event = getEvent(eventCode);
        return ticketRepository.findByEvent_EventCodeAndStatus(event.getEventCode(), TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    /** BR-TICKET-010..012: PAID -> CANCELLED. */
    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = getTicket(ticketCode);

        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Ticket %s is %s."
                            .formatted(ticketCode, ticket.getStatus()));
        }
        Event event = ticket.getEvent();
        if (!event.getEventDate().isAfter(now())) {
            throw new BusinessRuleException("Cannot cancel a ticket after the event date: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket saved = ticketRepository.save(ticket);

        // El ciclo de estados del PRD no permite SOLD_OUT -> PUBLISHED: el evento no se modifica.

        return ticketMapper.toResponse(saved);
    }

    /** BR-TICKET-013..014: PAID -> USED. */
    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = getTicket(ticketCode);

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("A CANCELLED ticket can never be used: " + ticketCode);
        }
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be marked as used. Ticket %s is %s."
                            .formatted(ticketCode, ticket.getStatus()));
        }

        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    /** BR-TICKET-006: la edad se evalua en la fecha del evento, no en la de compra. */
    private void validateMinimumAge(User user, Event event) {
        Integer minimumAge = event.getMinimumAge();
        if (minimumAge == null || minimumAge <= 0) {
            return;
        }
        LocalDate birthDate = Optional.ofNullable(user.getProfile())
                .map(UserProfile::getBirthDate)
                .orElseThrow(() -> new BusinessRuleException(
                        "User birth date is required to verify minimum age: " + user.getEmail()));

        int ageAtEvent = Period.between(birthDate, event.getEventDate().toLocalDate()).getYears();
        if (ageAtEvent < minimumAge) {
            throw new BusinessRuleException("User does not meet minimum age.");
        }
    }

    private Event getEvent(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    private Ticket getTicket(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
