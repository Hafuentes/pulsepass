package com.pulse.pass.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pulse.pass.domain.Artist;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /** FR-ART-001/002: recuperar un artista por su nombre artistico unico. */
    Optional<Artist> findByStageName(String stageName);

    /** Busqueda por nombre artistico ignorando mayusculas. */
    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    /** BR-ARTIST-002: artistas activos ordenados por nombre artistico. */
    List<Artist> findByActiveTrueOrderByStageNameAsc();
}
