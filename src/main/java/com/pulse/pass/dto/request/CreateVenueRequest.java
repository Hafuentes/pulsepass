package com.pulse.pass.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVenueRequest(
        @NotBlank(message = "Venue code is required") @Size(max = 50) String code,
        @NotBlank(message = "Venue name is required") @Size(max = 120) String name,
        @NotBlank(message = "City is required") @Size(max = 100) String city,
        @Size(max = 200) String address,
        @NotNull(message = "Capacity is required") @Min(value = 1, message = "Capacity must be greater than zero") Integer capacity
) {}
