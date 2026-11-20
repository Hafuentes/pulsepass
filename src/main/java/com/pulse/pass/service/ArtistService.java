package com.pulse.pass.service;

import java.util.List;

import com.pulse.pass.dto.response.ArtistResponse;
import com.pulse.pass.dto.request.CreateArtistRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ArtistService {

    ArtistResponse findById(Long id);

    @PreAuthorize("hasRole('ADMIN')")
    ArtistResponse create(CreateArtistRequest request);

    ArtistResponse findByStageName(String stageName);

    List<ArtistResponse> findActiveArtists();
}
