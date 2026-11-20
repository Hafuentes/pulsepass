package com.pulse.pass.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.dto.response.ArtistResponse;
import com.pulse.pass.dto.request.CreateArtistRequest;
import com.pulse.pass.domain.Artist;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.ArtistMapper;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.service.ArtistService;

@Service
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;

    public ArtistServiceImpl(ArtistRepository artistRepository, ArtistMapper artistMapper) {
        this.artistRepository = artistRepository;
        this.artistMapper = artistMapper;
    }

    @Override
    @Transactional
    public ArtistResponse create(CreateArtistRequest request) {
        String stageName = request.stageName().trim();
        if (artistRepository.existsByStageNameIgnoreCase(stageName)) {
            throw new DuplicateResourceException("Artist stage name already exists: " + stageName);
        }
        Artist artist = new Artist(stageName, request.country() == null ? null : request.country().trim(),
                request.genre() == null ? null : request.genre().trim(), true);
        return artistMapper.toResponse(artistRepository.save(artist));
    }

    /** BR-ARTIST-001 */
    @Override
    @Transactional(readOnly = true)
    public ArtistResponse findById(Long id) {
        return artistRepository.findById(id)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + id));
    }

    /** BR-ARTIST-001 */
    @Override
    @Transactional(readOnly = true)
    public ArtistResponse findByStageName(String stageName) {
        return artistRepository.findByStageNameIgnoreCase(stageName)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + stageName));
    }

    /** BR-ARTIST-002 */
    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponse> findActiveArtists() {
        return artistRepository.findByActiveTrueOrderByStageNameAsc()
                .stream()
                .map(artistMapper::toResponse)
                .toList();
    }
}
