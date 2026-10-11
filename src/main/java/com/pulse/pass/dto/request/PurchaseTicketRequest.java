package com.pulse.pass.dto.request;

import com.pulse.pass.domain.TicketType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Sin precio: el precio lo calcula el sistema (ver TicketPriceCalculator). */
public record PurchaseTicketRequest(
        @NotBlank(message = "User email is required")
        @Email(message = "User email must be a valid email") String userEmail,
        @NotBlank(message = "Event code is required") String eventCode,
        @NotNull(message = "Ticket type is required") TicketType type
) {
}
