package com.pulse.pass.dto.response;

import java.time.LocalDateTime;

import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;

/** Vista ligera para listados (sin artistas ni descripcion). */
public record EventSummaryResponse(
        Long id,
        String eventCode,
        String name,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        String venueCode,
        String venueName,
        String venueCity
) {
}
