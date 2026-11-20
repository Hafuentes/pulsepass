package com.pulse.pass.dto.request;

import com.pulse.pass.domain.EventStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeEventStatusRequest(
        @NotNull(message = "Event status is required") EventStatus status
) {
}
