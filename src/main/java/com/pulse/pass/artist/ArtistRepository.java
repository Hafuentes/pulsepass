package main.java.com.pulse.pass.artist;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /** FR-ART-001/002: recuperar un artista por su nombre artistico unico. */
    Optional<Artist> findByStageName(String stageName);
}
