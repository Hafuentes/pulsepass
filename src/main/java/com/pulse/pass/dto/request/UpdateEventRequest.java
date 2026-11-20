package com.pulse.pass.dto.request;

import java.time.LocalDateTime;

import com.pulse.pass.domain.EventCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Editable event details. The business code and lifecycle status remain unchanged. */
public record UpdateEventRequest(
        @NotBlank(message = "Name is required") String name,
        @Size(max = 2000, message = "Description must not exceed 2000 characters") String description,
        @NotNull(message = "Category is required") EventCategory category,
        @NotNull(message = "Event date is required") LocalDateTime eventDate,
        @NotNull(message = "Minimum age is required")
        @Min(value = 0, message = "Minimum age must be zero or greater") Integer minimumAge,
        @NotBlank(message = "Venue code is required") String venueCode
) {
}
