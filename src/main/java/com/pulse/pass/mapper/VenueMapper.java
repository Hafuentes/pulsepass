package com.pulse.pass.mapper;

import org.mapstruct.Mapper;

import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.response.VenueResponse;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    VenueResponse toResponse(Venue venue);
}
