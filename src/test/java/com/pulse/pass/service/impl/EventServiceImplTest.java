package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.request.CreateEventRequest;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.EventMapper;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 12, 0);
    private static final LocalDateTime FUTURE = LocalDateTime.of(2026, 12, 12, 20, 0);
    private static final LocalDateTime PAST = LocalDateTime.of(2026, 1, 1, 20, 0);
    private static final String CODE = "CMF-2026";
    private static final String VENUE_CODE = "VEN-SMR-01";

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private EventMapper eventMapper;

    private EventServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new EventServiceImpl(eventRepository, venueRepository, artistRepository, eventMapper, clock);
    }

    // ---------- helpers ----------

    private Venue venue(boolean active) {
        return new Venue(VENUE_CODE, "Marina Convention Center", "Santa Marta", "Calle 1", 3, active);
    }

    private Event event(EventStatus status, LocalDateTime date, Venue venue) {
        return new Event(CODE, "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                status, date, 18, null, venue);
    }

    private CreateEventRequest request(LocalDateTime date, Integer minimumAge) {
        return new CreateEventRequest(CODE, "Caribbean Music Fest 2026", "desc",
                EventCategory.MUSIC, date, minimumAge, VENUE_CODE);
    }

    private EventResponse response(EventStatus status) {
        return new EventResponse(1L, CODE, "Caribbean Music Fest 2026", "desc", EventCategory.MUSIC,
                status, FUTURE, 18, VENUE_CODE, "Marina Convention Center", List.of());
    }

    private Artist artist(long id, String name) {
        Artist artist = new Artist(name, "Colombia", "Pop", true);
        ReflectionTestUtils.setField(artist, "id", id);
        return artist;
    }

    // ---------- findByCode ----------

    @Test
    void findByCode_existingEvent_returnsDto() { // TEST-EVENT-001
        // ARRANGE
        Event event = event(EventStatus.PUBLISHED, FUTURE, venue(true));
        EventResponse expected = response(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = service.findByCode(CODE);

        // ASSERT
        assertThat(result).isEqualTo(expected);
        verify(eventRepository).findByEventCode(CODE);
    }

    @Test
    void findByCode_missingEvent_throwsResourceNotFound() { // TEST-EVENT-002
        // ARRANGE
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findByCode(CODE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: " + CODE);
    }

    @Test
    void findPublishedEvents_mapsEveryPublishedEventToSummary() {
        // ARRANGE
        Event event = event(EventStatus.PUBLISHED, FUTURE, venue(true));
        EventSummaryResponse summary = new EventSummaryResponse(1L, CODE, "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED, FUTURE, VENUE_CODE, "Marina Convention Center",
                "Santa Marta");
        when(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)).thenReturn(List.of(event));
        when(eventMapper.toSummary(event)).thenReturn(summary);

        // ACT
        List<EventSummaryResponse> result = service.findPublishedEvents();

        // ASSERT
        assertThat(result).containsExactly(summary);
    }

    // ---------- create ----------

    @Test
    void create_validRequest_savesEventAsDraft() { // TEST-EVENT-003 / AC-001
        // ARRANGE
        EventResponse expected = response(EventStatus.DRAFT);
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue(true)));
        when(eventRepository.save(any(Event.class))).then(returnsFirstArg());
        when(eventMapper.toResponse(any(Event.class))).thenReturn(expected);

        // ACT
        EventResponse result = service.create(request(FUTURE, 18));

        // ASSERT
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(captor.getValue().getEventCode()).isEqualTo(CODE);
        assertThat(captor.getValue().getMinimumAge()).isEqualTo(18);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void create_nullMinimumAge_defaultsToZero() {
        // ARRANGE
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue(true)));
        when(eventRepository.save(any(Event.class))).then(returnsFirstArg());

        // ACT
        service.create(request(FUTURE, null));

        // ASSERT
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getMinimumAge()).isZero();
    }

    @Test
    void create_duplicatedCode_throwsDuplicateAndNeverSaves() { // BR-EVENT-001
        // ARRANGE
        when(eventRepository.existsByEventCode(CODE)).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(FUTURE, 18)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_missingVenue_throwsResourceNotFoundAndNeverSaves() { // TEST-EVENT-004
        // ARRANGE
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(FUTURE, 18)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(VENUE_CODE);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void create_inactiveVenue_throwsBusinessRuleException() { // TEST-EVENT-005
        // ARRANGE
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue(false)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(FUTURE, 18)))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_pastDate_throwsBusinessRuleException() { // TEST-EVENT-006
        // ARRANGE
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue(true)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(PAST, 18)))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_negativeMinimumAge_throwsBusinessRuleException() { // BR-EVENT-006
        // ARRANGE
        when(venueRepository.findByCode(VENUE_CODE)).thenReturn(Optional.of(venue(true)));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.create(request(FUTURE, -1)))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- publish ----------

    @Test
    void publish_validDraft_becomesPublished() { // TEST-EVENT-007 / AC-002
        // ARRANGE
        Event event = event(EventStatus.DRAFT, FUTURE, venue(true));
        EventResponse expected = response(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = service.publish(CODE);

        // ASSERT
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
    }

    @Test
    void publish_cancelledEvent_throwsBusinessRuleAndDoesNotPersist() { // TEST-EVENT-008
        // ARRANGE
        Event event = event(EventStatus.CANCELLED, FUTURE, venue(true));
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish(CODE)).isInstanceOf(BusinessRuleException.class);
        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publish_alreadyPublishedSoldOutOrFinished_isRejected() { // BR-EVENT-007
        for (EventStatus status : List.of(EventStatus.PUBLISHED, EventStatus.SOLD_OUT, EventStatus.FINISHED)) {
            // ARRANGE
            Event event = event(status, FUTURE, venue(true));
            when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));

            // ACT + ASSERT
            assertThatThrownBy(() -> service.publish(CODE)).isInstanceOf(BusinessRuleException.class);
        }
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publish_pastDate_throwsBusinessRule() { // BR-EVENT-008
        // ARRANGE
        Event event = event(EventStatus.DRAFT, PAST, venue(true));
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish(CODE)).isInstanceOf(BusinessRuleException.class);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publish_inactiveVenue_throwsBusinessRule() { // BR-EVENT-009
        // ARRANGE
        Event event = event(EventStatus.DRAFT, FUTURE, venue(false));
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish(CODE)).isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publish_missingEvent_throwsResourceNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.publish(CODE)).isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- addArtist ----------

    @Test
    void addArtist_validArtist_isAssociatedAndSaved() { // AC-003
        // ARRANGE
        Event event = event(EventStatus.DRAFT, FUTURE, venue(true));
        Artist solarBeat = artist(1L, "Solar Beat");
        Artist neonWaves = artist(2L, "Neon Waves");
        event.addArtist(solarBeat);
        EventResponse expected = response(EventStatus.DRAFT);
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(2L)).thenReturn(Optional.of(neonWaves));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(expected);

        // ACT
        EventResponse result = service.addArtist(CODE, 2L);

        // ASSERT
        assertThat(event.getArtists()).containsExactlyInAnyOrder(solarBeat, neonWaves);
        assertThat(result).isEqualTo(expected);
        verify(eventRepository).save(event);
    }

    @Test
    void addArtist_sameArtistTwice_throwsDuplicateAndDoesNotSave() { // BR-EVENT-010
        // ARRANGE
        Event event = event(EventStatus.DRAFT, FUTURE, venue(true));
        Artist solarBeat = artist(1L, "Solar Beat");
        event.addArtist(solarBeat);
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(solarBeat));

        // ACT + ASSERT
        assertThatThrownBy(() -> service.addArtist(CODE, 1L)).isInstanceOf(DuplicateResourceException.class);
        assertThat(event.getArtists()).hasSize(1);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void addArtist_cancelledOrFinishedEvent_throwsBusinessRule() { // BR-EVENT-011
        for (EventStatus status : List.of(EventStatus.CANCELLED, EventStatus.FINISHED)) {
            // ARRANGE
            Event event = event(status, FUTURE, venue(true));
            when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));
            when(artistRepository.findById(eq(1L))).thenReturn(Optional.of(artist(1L, "Solar Beat")));

            // ACT + ASSERT
            assertThatThrownBy(() -> service.addArtist(CODE, 1L)).isInstanceOf(BusinessRuleException.class);
            assertThat(event.getArtists()).isEmpty();
        }
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void addArtist_missingArtist_throwsResourceNotFound() {
        // ARRANGE
        Event event = event(EventStatus.DRAFT, FUTURE, venue(true));
        when(eventRepository.findByEventCode(CODE)).thenReturn(Optional.of(event));
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.addArtist(CODE, 99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ---------- findByArtist ----------

    @Test
    void findByArtist_existingArtist_returnsSummaries() {
        // ARRANGE
        Artist artist = artist(1L, "Solar Beat");
        Event event = event(EventStatus.PUBLISHED, FUTURE, venue(true));
        EventSummaryResponse summary = new EventSummaryResponse(1L, CODE, "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED, FUTURE, VENUE_CODE, "Marina Convention Center",
                "Santa Marta");
        when(artistRepository.findByStageNameIgnoreCase("solar beat")).thenReturn(Optional.of(artist));
        when(eventRepository.findEventsByArtistStageName("Solar Beat")).thenReturn(List.of(event));
        when(eventMapper.toSummary(event)).thenReturn(summary);

        // ACT
        List<EventSummaryResponse> result = service.findByArtist("solar beat");

        // ASSERT
        assertThat(result).containsExactly(summary);
    }

    @Test
    void findByArtist_unknownArtist_throwsResourceNotFound() {
        // ARRANGE
        when(artistRepository.findByStageNameIgnoreCase("Ghost")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findByArtist("Ghost")).isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).findEventsByArtistStageName(any());
    }
}
