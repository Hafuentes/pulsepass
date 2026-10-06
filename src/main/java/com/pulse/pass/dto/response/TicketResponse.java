package com.pulse.pass.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;

public record TicketResponse(
        Long id,
        String ticketCode,
        TicketType type,
        BigDecimal price,
        TicketStatus status,
        LocalDateTime purchaseDate,
        String userEmail,
        String eventCode,
        String eventName
) {
}
