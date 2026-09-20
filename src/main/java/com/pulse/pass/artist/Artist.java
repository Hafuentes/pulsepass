package com.pulse.pass.artist;

import java.util.HashSet;
import java.util.Set;

import com.pulse.pass.event.Event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Artista que puede participar en multiples eventos (BR-003, relacion N:M con Event).
 */
@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "stage_name", nullable = false, unique = true, length = 100)
    private String stageName;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "genre", length = 50)
    private String genre;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Lado inverso de la relacion N:M; el dueno de la relacion es Event.artists. */
    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();

    protected Artist() {
        // Requerido por JPA
    }

    public Artist(String stageName, String country, String genre, boolean active) {
        this.stageName = stageName;
        this.country = country;
        this.genre = genre;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Event> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        return "Artist{id=%d, stageName='%s', country='%s', genre='%s'}"
                .formatted(id, stageName, country, genre);
    }
}
