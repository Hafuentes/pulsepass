package com.pulse.pass.dto.request;

import java.time.LocalDateTime;

import com.pulse.pass.domain.EventCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** El estado inicial (DRAFT) NO se recibe: lo decide el servicio (BR-EVENT-005). */
public record CreateEventRequest(
        @NotBlank String eventCode,
        @NotBlank String name,
        String description,
        @NotNull EventCategory category,
        @NotNull LocalDateTime eventDate,
        Integer minimumAge,
        @NotBlank String venueCode
) {
}
