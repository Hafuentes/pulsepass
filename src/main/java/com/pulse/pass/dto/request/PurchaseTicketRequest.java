package com.pulse.pass.dto.request;

import com.pulse.pass.domain.TicketType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Sin precio: el precio lo calcula el sistema (ver TicketPriceCalculator). */
public record PurchaseTicketRequest(
        @NotBlank @Email String userEmail,
        @NotBlank String eventCode,
        @NotNull TicketType type
) {
}
