package com.pulse.pass.service;

import java.util.List;

import com.pulse.pass.dto.response.ArtistResponse;

public interface ArtistService {

    ArtistResponse findById(Long id);

    ArtistResponse findByStageName(String stageName);

    List<ArtistResponse> findActiveArtists();
}
