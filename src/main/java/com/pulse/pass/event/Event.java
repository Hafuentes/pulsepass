package com.pulse.pass.event;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.pulse.pass.artist.Artist;
import com.pulse.pass.venue.Venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Evento asociado a exactamente un Venue (BR-001) y a cero o muchos Artist (BR-003).
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "event_code", nullable = false, unique = true, length = 50)
    private String eventCode;

    @NotBlank
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private EventCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EventStatus status;

    @NotNull
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "minimum_age")
    private Integer minimumAge = 0;

    /** FR-EVT-006: URL de streaming opcional (introducida en V3, columna NULL-able). */
    @Column(name = "streaming_url", length = 500)
    private String streamingUrl;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    /** Dueno de la relacion N:M event_artists (BR-003). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    protected Event() {
        // Requerido por JPA
    }

    public Event(String eventCode, String name, String description, EventCategory category,
                 EventStatus status, LocalDateTime eventDate, Integer minimumAge,
                 String streamingUrl, Venue venue) {
        this.eventCode = eventCode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.status = status;
        this.eventDate = eventDate;
        this.minimumAge = minimumAge;
        this.streamingUrl = streamingUrl;
        this.venue = venue;
    }

    /** Mantiene sincronizados ambos lados de la relacion N:M sin duplicar el par evento-artista. */
    public void addArtist(Artist artist) {
        if (this.artists.add(artist)) {
            artist.getEvents().add(this);
        }
    }

    public void removeArtist(Artist artist) {
        if (this.artists.remove(artist)) {
            artist.getEvents().remove(this);
        }
    }

    public Long getId() {
        return id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Integer getMinimumAge() {
        return minimumAge;
    }

    public void setMinimumAge(Integer minimumAge) {
        this.minimumAge = minimumAge;
    }

    public String getStreamingUrl() {
        return streamingUrl;
    }

    public void setStreamingUrl(String streamingUrl) {
        this.streamingUrl = streamingUrl;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public Set<Artist> getArtists() {
        return artists;
    }

    @Override
    public String toString() {
        return "Event{id=%d, eventCode='%s', name='%s', status=%s}"
                .formatted(id, eventCode, name, status);
    }
}
