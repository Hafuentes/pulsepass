package com.pulse.pass.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

import com.pulse.pass.security.RestAccessDeniedHandler;
import com.pulse.pass.security.RestAuthenticationEntryPoint;
import com.pulse.pass.security.SecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.VenueService;

@WebMvcTest(VenueController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class VenueControllerTest extends AbstractSecuredControllerTest {

    @MockitoBean
    private VenueService venueService;

    private VenueResponse venue() {
        return new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta",
                "Calle 1", 3, true);
    }

    @Test
    void TEST_CTRL_VEN_001_findByCode_existing_returns200() throws Exception {
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(venue());

        mockMvc.perform(get("/api/venues/VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.capacity").value(3));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    @Test
    void TEST_CTRL_VEN_002_findByCode_missing_returns404() throws Exception {
        when(venueService.findByCode("NOPE"))
                .thenThrow(new ResourceNotFoundException("Venue not found: NOPE"));

        mockMvc.perform(get("/api/venues/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: NOPE"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void TEST_CTRL_VEN_003_findActiveVenues_returns200() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of(venue()));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(venueService).findActiveVenues();
    }
}
