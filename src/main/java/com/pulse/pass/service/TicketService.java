package com.pulse.pass.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

import com.pulse.pass.dto.request.PurchaseTicketRequest;
import com.pulse.pass.dto.response.TicketResponse;

public interface TicketService {

    @PreAuthorize("@authz.canPurchaseFor(#request.userEmail())")
    TicketResponse purchase(PurchaseTicketRequest request);

    @PreAuthorize("@authz.canAccessTicket(#ticketCode)")
    TicketResponse findByCode(String ticketCode);

    @PreAuthorize("@authz.canListTicketsOf(#email)")
    List<TicketResponse> findByUserEmail(String email);

    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    @PreAuthorize("@authz.canCancelTicket(#ticketCode)")
    TicketResponse cancel(String ticketCode);

    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    TicketResponse markAsUsed(String ticketCode);
}
