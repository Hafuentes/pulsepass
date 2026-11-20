package com.pulse.pass.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.request.CreateEventRequest;
import com.pulse.pass.dto.request.ChangeEventStatusRequest;
import com.pulse.pass.dto.request.UpdateEventRequest;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.EventMapper;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.VenueRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.service.EventService;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;
    private final Clock clock;

    @Autowired
    public EventServiceImpl(EventRepository eventRepository,
                            TicketRepository ticketRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper eventMapper,
                            Clock clock) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    /** Compatibility constructor for focused unit tests of operations that do not inspect tickets. */
    public EventServiceImpl(EventRepository eventRepository,
                            VenueRepository venueRepository,
                            ArtistRepository artistRepository,
                            EventMapper eventMapper,
                            Clock clock) {
        this(eventRepository, null, venueRepository, artistRepository, eventMapper, clock);
    }

    @Override
    @Transactional
    public EventResponse update(String eventCode, UpdateEventRequest request) {
        Event event = getEvent(eventCode);
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException("Cannot edit a %s event: %s.".formatted(event.getStatus(), eventCode));
        }
        if (ticketRepository.existsByEvent_EventCode(eventCode)) {
            throw new BusinessRuleException("Cannot edit an event that has tickets. Cancel it instead.");
        }
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));
        if (!venue.isActive()) {
            throw new BusinessRuleException("Venue is not active: " + venue.getCode());
        }
        if (!isFuture(request.eventDate())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }
        event.setName(request.name());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setEventDate(request.eventDate());
        event.setMinimumAge(request.minimumAge());
        event.setVenue(venue);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    /** BR-EVENT-001..006 */
    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event already exists: " + request.eventCode());
        }

        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));

        if (!venue.isActive()) {
            throw new BusinessRuleException("Venue is not active: " + venue.getCode());
        }
        if (!isFuture(request.eventDate())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }

        int minimumAge = request.minimumAge() == null ? 0 : request.minimumAge();
        if (minimumAge < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative.");
        }

        // BR-EVENT-005: el estado inicial siempre es DRAFT, el request no lo controla.
        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.category(),
                EventStatus.DRAFT,
                request.eventDate(),
                minimumAge,
                null,
                venue);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(String eventCode) {
        return eventMapper.toResponse(getEvent(eventCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    /** BR-EVENT-007..009: DRAFT -> PUBLISHED. */
    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findAllForAdmin() {
        return eventRepository.findAllByOrderByEventDateAsc()
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = getEvent(eventCode);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Event %s is %s."
                            .formatted(eventCode, event.getStatus()));
        }
        if (!isFuture(event.getEventDate())) {
            throw new BusinessRuleException("Cannot publish an event whose date is not in the future: " + eventCode);
        }
        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException("Cannot publish: venue is not active: " + event.getVenue().getCode());
        }

        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse changeStatus(String eventCode, ChangeEventStatusRequest request) {
        Event event = getEvent(eventCode);
        EventStatus current = event.getStatus();
        EventStatus target = request.status();
        boolean validTransition = (current == EventStatus.PUBLISHED
                && (target == EventStatus.SOLD_OUT || target == EventStatus.FINISHED || target == EventStatus.CANCELLED))
                || (current == EventStatus.SOLD_OUT
                && (target == EventStatus.FINISHED || target == EventStatus.CANCELLED));
        if (!validTransition) {
            throw new BusinessRuleException(
                    "Cannot change event status from %s to %s.".formatted(current, target));
        }
        event.setStatus(target);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    /** BR-EVENT-010..011 */
    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = getEvent(eventCode);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));

        if (isAlreadyAssociated(event, artist)) {
            throw new DuplicateResourceException(
                    "Artist %s is already associated with event %s.".formatted(artist.getStageName(), eventCode));
        }

        event.addArtist(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse removeArtist(String eventCode, Long artistId) {
        Event event = getEvent(eventCode);
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));
        if (!isAlreadyAssociated(event, artist)) {
            throw new BusinessRuleException(
                    "Artist %s is not associated with event %s.".formatted(artist.getStageName(), eventCode));
        }
        event.removeArtist(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(String stageName) {
        Artist artist = artistRepository.findByStageNameIgnoreCase(stageName)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + stageName));

        return eventRepository.findEventsByArtistStageName(artist.getStageName())
                .stream()
                .map(eventMapper::toSummary)
                .collect(Collectors.toCollection(ArrayList::new)); // mutable: requerido por @PostFilter
    }

    private Event getEvent(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    private boolean isFuture(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(LocalDateTime.now(clock));
    }

    private static boolean isAlreadyAssociated(Event event, Artist artist) {
        return event.getArtists().contains(artist)
                || event.getArtists().stream()
                        .anyMatch(a -> a.getId() != null && a.getId().equals(artist.getId()));
    }
}
