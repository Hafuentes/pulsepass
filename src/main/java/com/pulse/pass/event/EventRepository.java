package com.pulse.pass.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** FR-EVT-002: recuperar un evento por su eventCode unico. */
    Optional<Event> findByEventCode(String eventCode);

    /** FR-EVT-005: eventos PUBLISHED ordenados por fecha ascendente. */
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    /** FR-VEN-004: eventos de un venue navegando la relacion por su codigo de negocio. */
    List<Event> findByVenue_CodeOrderByEventDateAsc(String venueCode);

    /** FR-ART-004 / FR-SRC-001: eventos en los que participa un artista (JOIN, sin duplicados). */
    @Query("SELECT DISTINCT e FROM Event e JOIN e.artists a WHERE a.stageName = :stageName")
    List<Event> findEventsByArtistStageName(@Param("stageName") String stageName);

    /** FR-SRC-002: eventos de una ciudad en los que participa un artista especifico. */
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.artists a
            WHERE e.venue.city = :city AND a.stageName = :stageName
            """)
    List<Event> findEventsByCityAndArtistStageName(@Param("city") String city,
                                                     @Param("stageName") String stageName);

    /**
     * FR-SRC-003: eventos recomendados: PUBLISHED, posteriores a una fecha, en una ciudad
     * y cuyo artista contenga el texto buscado (case-insensitive, sin duplicados, ordenados por fecha).
     */
    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.artists a
            WHERE e.status = com.pulse.pass.event.EventStatus.PUBLISHED
              AND e.eventDate > :afterDate
              AND e.venue.city = :city
              AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistNameFragment, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> findRecommendedEvents(@Param("city") String city,
                                       @Param("artistNameFragment") String artistNameFragment,
                                       @Param("afterDate") LocalDateTime afterDate);
}
