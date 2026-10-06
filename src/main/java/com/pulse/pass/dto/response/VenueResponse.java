package com.pulse.pass.dto.response;

public record VenueResponse(
        Long id,
        String code,
        String name,
        String city,
        String address,
        Integer capacity,
        boolean active
) {
}
