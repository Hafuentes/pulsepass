package com.pulse.pass.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;

public record EventResponse(
        Long id,
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        Integer minimumAge,
        String venueCode,
        String venueName,
        List<ArtistResponse> artists
) {
}
