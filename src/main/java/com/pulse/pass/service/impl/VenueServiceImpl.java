package com.pulse.pass.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.dto.request.CreateVenueRequest;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.VenueMapper;
import com.pulse.pass.repository.VenueRepository;
import com.pulse.pass.service.VenueService;

@Service
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public VenueServiceImpl(VenueRepository venueRepository, VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
    }

    @Override
    @Transactional
    public VenueResponse create(CreateVenueRequest request) {
        String code = request.code().trim().toUpperCase();
        if (venueRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Venue code already exists: " + code);
        }
        Venue venue = new Venue(code, request.name().trim(), request.city().trim(),
                request.address() == null ? null : request.address().trim(), request.capacity(), true);
        return venueMapper.toResponse(venueRepository.save(venue));
    }

    /** BR-VENUE-001 */
    @Override
    @Transactional(readOnly = true)
    public VenueResponse findByCode(String code) {
        return venueRepository.findByCode(code)
                .map(venueMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + code));
    }

    /** BR-VENUE-002 */
    @Override
    @Transactional(readOnly = true)
    public List<VenueResponse> findActiveVenues() {
        return venueRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(venueMapper::toResponse)
                .toList();
    }
}
