package com.pulse.pass.service;

import java.util.List;

import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.dto.request.CreateVenueRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public interface VenueService {

    VenueResponse findByCode(String code);

    @PreAuthorize("hasRole('ADMIN')")
    VenueResponse create(CreateVenueRequest request);

    List<VenueResponse> findActiveVenues();
}
