package com.pulse.pass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import com.pulse.pass.domain.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findByCode(String code);

    boolean existsByCode(String code);

    List<Venue> findByActiveTrue();

    /** BR-VENUE-002: venues activos ordenados por nombre. */
    List<Venue> findByActiveTrueOrderByNameAsc();
}
