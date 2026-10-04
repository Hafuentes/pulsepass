package com.pulse.pass.service;

import java.util.List;

import com.pulse.pass.dto.response.VenueResponse;

public interface VenueService {

    VenueResponse findByCode(String code);

    List<VenueResponse> findActiveVenues();
}
