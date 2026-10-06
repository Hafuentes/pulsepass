package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserProfile;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.request.PurchaseTicketRequest;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.TicketMapper;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.service.TicketPriceCalculator;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);
    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 12, 12, 20, 0);
    private static final String EVENT_CODE = "CMF-2026";
    private static final String EMAIL = "andrea@email.com";
    private static final String TICKET_CODE = "TKT-0001";
    private static final BigDecimal VIP_PRICE = new BigDecimal("250.00");

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketPriceCalculator priceCalculator;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new TicketServiceImpl(ticketRepository, userRepository, eventRepository,
                ticketMapper, priceCalculator, clock);
    }

    // ---------- helpers ----------

    private User user(String email, boolean active, LocalDate birthDate) {
        User user = new User(email.substring(0, email.indexOf('@')), email, active);
        user.assignProfile(new UserProfile("Name", "Surname", null, "Santa Marta", birthDate, user));
        return user;
    }

    /** 25 anos en la fecha del evento. */
    private User adult() {
        return user(EMAIL, true, LocalDate.of(2001, 1, 1));
    }

    private Event event(EventStatus status, LocalDateTime date, int capacity, int minimumAge) {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1", capacity, true);
        return new Event(EVENT_CODE, "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                status, date, minimumAge, null, venue);
    }

    private Ticket ticket(TicketStatus status, Event event) {
        return new Ticket(TICKET_CODE, TicketType.GENERAL, new BigDecimal("100.00"), status,
                NOW.minusDays(1), adult(), event);
    }

    private PurchaseTicketRequest vipRequest() {
        return new PurchaseTicketRequest(EMAIL, EVENT_CODE, TicketType.VIP);
    }

    private TicketResponse response(TicketStatus status) {
        return new TicketResponse(1L, TICKET_CODE, TicketType.VIP, VIP_PRICE, status, NOW,
                EMAIL, EVENT_CODE, "Caribbean Music Fest 2026");
    }

    // ---------- purchase: caminos validos ----------

    @Test
    void purchase_validRequest_createsPaidTicket() { // TEST-TICKET-001 / AC-004
        // ARRANGE
        User user = adult();
        Event event = event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18);
        TicketResponse expected = response(TicketStatus.PAID);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countPaidTicketsByEventCode(EVENT_CODE)).thenReturn(0L);
        when(priceCalculator.calculate(TicketType.VIP)).thenReturn(VIP_PRICE);
        when(ticketRepository.save(any(Ticket.class))).then(returnsFirstArg());
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(expected);

        // ACT
        TicketResponse result = service.purchase(vipRequest());

        // ASSERT
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(saved.getType()).isEqualTo(TicketType.VIP);
        assertThat(saved.getPrice()).isEqualByComparingTo(VIP_PRICE);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getEvent()).isSameAs(event);
        assertThat(saved.getPurchaseDate()).isEqualTo(NOW);
        assertThat(saved.getTicketCode()).startsWith("TKT-");
        assertThat(result).isEqualTo(expected);
        // Aun quedan cupos: el evento NO pasa a SOLD_OUT.
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void purchase_lastAvailableTicket_savesTicketAndMarksEventSoldOut() { // TEST-TICKET-008 / AC-008
        // ARRANGE
        Event event = event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(adult()));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countPaidTicketsByEventCode(EVENT_CODE)).thenReturn(2L);
        when(priceCalculator.calculate(TicketType.VIP)).thenReturn(VIP_PRICE);
        when(ticketRepository.save(any(Ticket.class))).then(returnsFirstArg());

        // ACT
        service.purchase(vipRequest());

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventRepository).save(event);
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
    }

    @Test
    void purchase_ageIsEvaluatedAtEventDate_notAtPurchaseDate() { // BR-TICKET-006
        // ARRANGE: cumple 18 el 2026-11-01; hoy (2026-10-04) tiene 17, pero el evento es el 2026-12-12.
        User almostAdult = user(EMAIL, true, LocalDate.of(2008, 11, 1));
        Event event = event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(almostAdult));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countPaidTicketsByEventCode(EVENT_CODE)).thenReturn(0L);
        when(priceCalculator.calculate(TicketType.VIP)).thenReturn(VIP_PRICE);
        when(ticketRepository.save(any(Ticket.class))).then(returnsFirstArg());

        // ACT
        service.purchase(vipRequest());

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void purchase_eventWithoutAgeRestriction_ignoresBirthDate() {
        // ARRANGE: minimumAge = 0 -> no se exige perfil ni fecha de nacimiento.
        User noBirthDate = user(EMAIL, true, null);
        Event event = event(EventStatus.PUBLISHED, EVENT_DATE, 3, 0);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(noBirthDate));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countPaidTicketsByEventCode(EVENT_CODE)).thenReturn(0L);
        when(priceCalculator.calculate(TicketType.VIP)).thenReturn(VIP_PRICE);
        when(ticketRepository.save(any(Ticket.class))).then(returnsFirstArg());

        // ACT
        service.purchase(vipRequest());

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
    }

    // ---------- purchase: caminos invalidos ----------

    @Test
    void purchase_missingUser_throwsResourceNotFound() { // TEST-TICKET-002
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(vipRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(eventRepository, never()).findByEventCode(any());
    }

    @Test
    void purchase_inactiveUser_throwsBusinessRule() { // TEST-TICKET-003 / AC-007
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user(EMAIL, false, LocalDate.of(1996, 1, 1))));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(vipRequest())).isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(eventRepository, never()).findByEventCode(any());
    }

    @Test
    void purchase_missingEvent_throwsResourceNotFound() { // BR-TICKET-003
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(adult()));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(vipRequest())).isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void purchase_draftEvent_throwsBusinessRule() { // TEST-TICKET-004
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(adult()));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(event(EventStatus.DRAFT, EVENT_DATE, 3, 18)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(vipRequest())).isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void purchase_cancelledEvent_throwsBusinessRule() { // TEST-TICKET-005
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(adult()));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(event(EventStatus.CANCELLED, EVENT_DATE, 3, 18)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(vipRequest())).isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void purchase_soldOutOrFinishedEvent_throwsBusinessRule() { // BR-TICKET-004
        for (EventStatus status : List.of(EventStatus.SOLD_OUT, EventStatus.FINISHED)) {
            // ARRANGE
            when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(adult()));
            when(eventRepository.findByEventCode(EVENT_CODE))
                    .thenReturn(Optional.of(event(status, EVENT_DATE, 3, 18)));

            // ACT + ASSERT
            assertThatThrownBy(() -> service.purchase(vipRequest())).isInstanceOf(BusinessRuleException.class);
        }
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void purchase_eventAlreadyHappened_throwsBusinessRule() { // BR-TICKET-005
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(adult()));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(event(EventStatus.PUBLISHED, NOW.minusDays(1), 3, 18)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(vipRequest())).isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void purchase_underageUser_throwsBusinessRule() { // TEST-TICKET-006 / AC-006
        // ARRANGE: Laura, 17 anos en la fecha del evento.
        User laura = user("laura@email.com", true, LocalDate.of(2009, 6, 1));
        when(userRepository.findByEmailIgnoreCase("laura@email.com")).thenReturn(Optional.of(laura));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(
                new PurchaseTicketRequest("laura@email.com", EVENT_CODE, TicketType.GENERAL)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User does not meet minimum age.");
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(ticketRepository, never()).countPaidTicketsByEventCode(any());
    }

    @Test
    void purchase_ageRestrictedEventAndNoBirthDate_throwsBusinessRule() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user(EMAIL, true, null)));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(vipRequest())).isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void purchase_noCapacityLeft_throwsBusinessRule() { // TEST-TICKET-007 / AC-009
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(adult()));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18)));
        when(ticketRepository.countPaidTicketsByEventCode(EVENT_CODE)).thenReturn(3L);

        // ACT + ASSERT
        assertThatThrownBy(() -> service.purchase(vipRequest())).isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(eventRepository, never()).save(any(Event.class));
        verify(priceCalculator, never()).calculate(any());
    }

    // ---------- cancel ----------

    @Test
    void cancel_paidTicket_becomesCancelled() { // TEST-TICKET-009
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.PAID, event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18));
        TicketResponse expected = response(TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT
        TicketResponse result = service.cancel(TICKET_CODE);

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(result).isEqualTo(expected);
        verify(ticketRepository).save(ticket);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void cancel_ticketOfSoldOutEvent_doesNotChangeEventStatus() {
        // ARRANGE: el PRD no define la transicion SOLD_OUT -> PUBLISHED.
        Event event = event(EventStatus.SOLD_OUT, EVENT_DATE, 3, 18);
        Ticket ticket = ticket(TicketStatus.PAID, event);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        // ACT
        service.cancel(TICKET_CODE);

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void cancel_usedTicket_throwsBusinessRule() { // TEST-TICKET-010 / AC-011
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.USED, event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18));
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.cancel(TICKET_CODE)).isInstanceOf(BusinessRuleException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void cancel_alreadyCancelledTicket_throwsBusinessRule() { // BR-TICKET-011
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.CANCELLED, event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18));
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.cancel(TICKET_CODE)).isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void cancel_afterEventDate_throwsBusinessRule() { // BR-TICKET-012
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.PAID, event(EventStatus.PUBLISHED, NOW.minusHours(1), 3, 18));
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.cancel(TICKET_CODE)).isInstanceOf(BusinessRuleException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void cancel_missingTicket_throwsResourceNotFound() {
        // ARRANGE
        when(ticketRepository.findByTicketCode("NOPE")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.cancel("NOPE")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- markAsUsed ----------

    @Test
    void markAsUsed_paidTicket_becomesUsed() { // TEST-TICKET-011 / AC-010
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.PAID, event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18));
        TicketResponse expected = response(TicketStatus.USED);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT
        TicketResponse result = service.markAsUsed(TICKET_CODE);

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(result).isEqualTo(expected);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRule() { // TEST-TICKET-012 / BR-TICKET-014
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.CANCELLED, event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18));
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.markAsUsed(TICKET_CODE)).isInstanceOf(BusinessRuleException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void markAsUsed_alreadyUsedTicket_throwsBusinessRule() { // BR-TICKET-013
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.USED, event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18));
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.markAsUsed(TICKET_CODE)).isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    // ---------- consultas ----------

    @Test
    void findByCode_existing_returnsDto() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.PAID, event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18));
        TicketResponse expected = response(TicketStatus.PAID);
        when(ticketRepository.findByTicketCode(TICKET_CODE)).thenReturn(Optional.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT + ASSERT
        assertThat(service.findByCode(TICKET_CODE)).isEqualTo(expected);
    }

    @Test
    void findByCode_missing_throwsResourceNotFound() {
        when(ticketRepository.findByTicketCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("NOPE")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByUserEmail_returnsMappedTickets() {
        // ARRANGE
        Ticket ticket = ticket(TicketStatus.PAID, event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18));
        TicketResponse expected = response(TicketStatus.PAID);
        when(ticketRepository.findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc(EMAIL)).thenReturn(List.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT + ASSERT
        assertThat(service.findByUserEmail(EMAIL)).containsExactly(expected);
    }

    @Test
    void findPaidTicketsByEvent_existingEvent_returnsOnlyPaidTickets() {
        // ARRANGE
        Event event = event(EventStatus.PUBLISHED, EVENT_DATE, 3, 18);
        Ticket ticket = ticket(TicketStatus.PAID, event);
        TicketResponse expected = response(TicketStatus.PAID);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.findByEvent_EventCodeAndStatus(eq(EVENT_CODE), eq(TicketStatus.PAID)))
                .thenReturn(List.of(ticket));
        when(ticketMapper.toResponse(ticket)).thenReturn(expected);

        // ACT + ASSERT
        assertThat(service.findPaidTicketsByEvent(EVENT_CODE)).containsExactly(expected);
    }

    @Test
    void findPaidTicketsByEvent_missingEvent_throwsResourceNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findPaidTicketsByEvent(EVENT_CODE))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ticketRepository, never()).findByEvent_EventCodeAndStatus(any(), any());
    }
}
