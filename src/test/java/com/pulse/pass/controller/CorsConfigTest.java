package com.pulse.pass.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.pulse.pass.security.RestAccessDeniedHandler;
import com.pulse.pass.security.RestAuthenticationEntryPoint;
import com.pulse.pass.security.SecurityConfig;
import com.pulse.pass.service.VenueService;

/** NFR-SEC008: CORS parametrizado, con la cadena de filtros real (el preflight no exige token). */
@WebMvcTest(VenueController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = "pulsepass.security.cors.allowed-origins=https://app.example.com")
class CorsConfigTest extends AbstractSecuredControllerTest {

    @MockitoBean
    private VenueService venueService;

    @Test
    void preflightFromAllowedOrigin_isAcceptedWithoutToken() throws Exception {
        mockMvc.perform(options("/api/venues/active")
                        .header("Origin", "https://app.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.com"));
    }

    @Test
    void preflightFromUnknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/api/venues/active")
                        .header("Origin", "https://evil.example.org")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void actualRequestFromAllowedOrigin_carriesCorsHeader_andNoCredentialsAreAllowed() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of());

        mockMvc.perform(get("/api/venues/active").header("Origin", "https://app.example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.example.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
}
