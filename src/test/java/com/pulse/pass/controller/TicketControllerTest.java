package com.pulse.pass.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.dto.request.PurchaseTicketRequest;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.TicketService;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private TicketResponse ticket(TicketStatus status) {
        return new TicketResponse(1L, "TKT-001", TicketType.GENERAL, new BigDecimal("100.00"), status,
                LocalDateTime.of(2026, 10, 4, 10, 0), "andrea@mail.com", "CMF-2026",
                "Caribbean Music Fest 2026");
    }

    private static final String VALID_BODY = """
            {
              "userEmail": "andrea@mail.com",
              "eventCode": "CMF-2026",
              "type": "GENERAL"
            }
            """;

    @Test
    void TEST_CTRL_TKT_001_purchase_valid_returns201() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class))).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.price").value(100.00));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void TEST_CTRL_TKT_002_purchase_invalid_returns400() throws Exception {
        String invalid = """
                { "userEmail": "bad-email" }
                """;

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").exists())
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    void purchase_unknownTicketType_returns400() throws Exception {
        String invalid = VALID_BODY.replace("GENERAL", "PLATINUM");

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    void TEST_CTRL_TKT_003_purchase_userNotFound_returns404() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@mail.com"));

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: andrea@mail.com"));
    }

    @Test
    void TEST_CTRL_TKT_004_purchase_businessRule_returns409() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet the minimum age."));

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet the minimum age."));
    }

    @Test
    void TEST_CTRL_TKT_005_findByCode_returns200() throws Exception {
        when(ticketService.findByCode("TKT-001")).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TKT-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-001"));

        verify(ticketService).findByCode("TKT-001");
    }

    @Test
    void findByCode_missing_returns404() throws Exception {
        when(ticketService.findByCode("NOPE"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: NOPE"));

        mockMvc.perform(get("/api/tickets/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found: NOPE"));
    }

    @Test
    void TEST_CTRL_TKT_006_findByUser_returns200() throws Exception {
        when(ticketService.findByUserEmail("andrea@mail.com")).thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@mail.com"));

        verify(ticketService).findByUserEmail("andrea@mail.com");
        verify(ticketService, never()).findByCode(any());
    }

    @Test
    void TEST_CTRL_TKT_007_findPaidByEvent_returns200() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    @Test
    void TEST_CTRL_TKT_008_cancel_valid_returns200() throws Exception {
        when(ticketService.cancel("TKT-001")).thenReturn(ticket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TKT-001/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TKT-001");
    }

    @Test
    void TEST_CTRL_TKT_009_cancel_invalid_returns409() throws Exception {
        when(ticketService.cancel("TKT-001"))
                .thenThrow(new BusinessRuleException("A USED ticket cannot be cancelled."));

        mockMvc.perform(patch("/api/tickets/TKT-001/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A USED ticket cannot be cancelled."));
    }

    @Test
    void TEST_CTRL_TKT_010_use_valid_returns200() throws Exception {
        when(ticketService.markAsUsed("TKT-001")).thenReturn(ticket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TKT-001/use"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TKT-001");
    }

    @Test
    void TEST_CTRL_TKT_011_use_invalid_returns409() throws Exception {
        when(ticketService.markAsUsed("TKT-001"))
                .thenThrow(new BusinessRuleException("A CANCELLED ticket cannot be used."));

        mockMvc.perform(patch("/api/tickets/TKT-001/use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A CANCELLED ticket cannot be used."));
    }
}
