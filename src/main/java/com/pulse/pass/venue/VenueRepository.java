package com.pulse.pass.venue;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findByCode(String code);

    boolean existsByCode(String code);

    List<Venue> findByActiveTrue();
}