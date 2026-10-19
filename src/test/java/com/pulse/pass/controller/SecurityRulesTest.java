package com.pulse.pass.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.pulse.pass.dto.response.AuthResponse;
import com.pulse.pass.dto.response.AuthenticatedUserResponse;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.security.InvalidTokenException;
import com.pulse.pass.security.RestAccessDeniedHandler;
import com.pulse.pass.security.RestAuthenticationEntryPoint;
import com.pulse.pass.security.SecurityConfig;
import com.pulse.pass.security.SecurityPrincipal;
import com.pulse.pass.service.ArtistService;
import com.pulse.pass.service.AuthService;
import com.pulse.pass.service.EventService;
import com.pulse.pass.service.TicketService;
import com.pulse.pass.service.UserService;
import com.pulse.pass.service.VenueService;

/**
 * Matriz de seguridad HTTP de las rutas (T-SEC-001..008, 012..014, 016, 019) con la cadena de filtros real.
 * La propiedad (anti-IDOR) se prueba en AuthorizationServiceTest y SecurityFlowIT.
 */
@WebMvcTest
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SecurityRulesTest extends AbstractSecuredControllerTest {

    @MockitoBean
    private VenueService venueService;
    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private ArtistService artistService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private AuthService authService;

    private static final String EVENT_BODY = """
            {"eventCode":"E-1","name":"Fest","category":"MUSIC","eventDate":"2030-12-01T20:00:00",
             "minimumAge":0,"venueCode":"VEN-1"}
            """;

    private static final String REGISTER_BODY = """
            {"username":"andrea","email":"andrea@mail.com","firstName":"Andrea","lastName":"Lopez",
             "birthDate":"1995-05-20","password":"UnaFraseSeguraLarga2026!"}
            """;

    private SecurityPrincipal principal(boolean active, String... roles) {
        List<GrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
        return new SecurityPrincipal(7L, "andrea", "andrea@mail.com", "hash", active, authorities);
    }

    // ---------- rutas publicas ----------

    @Test
    void T_SEC_001_anonymous_publishedEvents_returns200() throws Exception {
        mockMvc.perform(get("/api/events/published")).andExpect(status().isOk());
    }

    @Test
    void anonymous_publicGets_returns200() throws Exception {
        when(authz.canViewEvent(any())).thenReturn(true);
        when(authz.canViewEventSummary(any())).thenReturn(true);
        mockMvc.perform(get("/api/venues/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/venues/VEN-SMR-01")).andExpect(status().isOk());
        mockMvc.perform(get("/api/artists/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/artists/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/events/CMF-2026")).andExpect(status().isOk());
        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat")).andExpect(status().isOk());
    }

    @Test
    void anonymous_register_isPublic_andDoesNotLeakPassword() throws Exception {
        when(authService.register(any())).thenReturn(new UserResponse(1L, "andrea", "andrea@mail.com", true,
                "Andrea", "Lopez", null, null, LocalDate.of(1995, 5, 20)));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.replace("UnaFraseSeguraLarga2026!", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void register_passwordOver72Bytes_returns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.replace("UnaFraseSeguraLarga2026!", "ñ".repeat(40))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    // ---------- login ----------

    @Test
    void T_SEC_005_login_isPublic_andReturnsBearerToken() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("jwt", "Bearer", 900));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"andrea\",\"password\":\"UnaFraseSeguraLarga2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void T_SEC_006_login_wrongPassword_returns401Generic() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"andrea\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.identifier").exists())
                .andExpect(jsonPath("$.details.password").exists());
        verifyNoInteractions(authService);
    }

    // ---------- 401 / 403 ----------

    @Test
    void T_SEC_002_anonymous_createEvent_returns401WithErrorResponse() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").exists());
        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void T_SEC_003_user_createEvent_returns403WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación"))
                .andExpect(jsonPath("$.details").exists());
        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void T_SEC_004_admin_createEvent_returns201() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isCreated());
        verify(eventService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void T_SEC_018_admin_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = {"USER", "STAFF"})
    void staffAndUser_cannotPublishOrAddArtists() throws Exception {
        mockMvc.perform(patch("/api/events/CMF-2026/publish")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events/CMF-2026/artists/1")).andExpect(status().isForbidden());
        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void T_SEC_014_admin_publishAndAddArtist_allowed() throws Exception {
        mockMvc.perform(patch("/api/events/CMF-2026/publish")).andExpect(status().isOk());
        mockMvc.perform(post("/api/events/CMF-2026/artists/1")).andExpect(status().isOk());
        verify(eventService).publish("CMF-2026");
        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void T_SEC_012_user_cannotMarkTicketAsUsed_orListPaidTickets() throws Exception {
        mockMvc.perform(patch("/api/tickets/TKT-1/use")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid")).andExpect(status().isForbidden());
        verifyNoInteractions(ticketService);
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void T_SEC_013_staff_canMarkTicketAsUsed_andListPaid() throws Exception {
        mockMvc.perform(patch("/api/tickets/TKT-1/use")).andExpect(status().isOk());
        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid")).andExpect(status().isOk());
        verify(ticketService).markAsUsed("TKT-1");
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staff_cannotPurchaseTickets() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"a@mail.com\",\"eventCode\":\"E\",\"type\":\"GENERAL\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ticketService);
    }

    @Test
    void anonymous_protectedRoutes_return401() throws Exception {
        mockMvc.perform(get("/api/tickets/TKT-1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "a@mail.com")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/tickets/TKT-1/cancel")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/by-email").param("email", "a@mail.com")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/by-username").param("username", "a")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(ticketService, userService);
    }

    @Test
    void unknownRoute_isDeniedByDefault_forAnonymous() throws Exception {
        mockMvc.perform(get("/api/internal/secret")).andExpect(status().isUnauthorized());
    }

    // ---------- Bearer / JWT ----------

    @Test
    void T_SEC_007_008_invalidToken_returns401WithoutCallingServices() throws Exception {
        when(jwtService.parseUserId("expired-or-tampered")).thenThrow(new InvalidTokenException("bad"));

        mockMvc.perform(get("/api/tickets/TKT-1").header("Authorization", "Bearer expired-or-tampered"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
        verifyNoInteractions(ticketService);
    }

    @Test
    void malformedAuthorizationHeader_doesNotProduce500() throws Exception {
        mockMvc.perform(get("/api/tickets/TKT-1").header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/TKT-1").header("Authorization", "Bearer    "))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/TKT-1").header("Authorization", "garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInQueryString_isNotAccepted() throws Exception {
        when(jwtService.parseUserId("good")).thenReturn(7L);
        when(userDetailsService.loadPrincipalById(7L)).thenReturn(principal(true, "ROLE_USER"));

        mockMvc.perform(get("/api/auth/me").param("access_token", "good"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validBearer_authenticatesAndMeReturnsMinimalData() throws Exception {
        when(jwtService.parseUserId("good")).thenReturn(7L);
        when(userDetailsService.loadPrincipalById(7L)).thenReturn(principal(true, "ROLE_USER"));
        when(authService.me(any())).thenReturn(
                new AuthenticatedUserResponse(7L, "andrea", "andrea@mail.com", List.of("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void T_SEC_016_validTokenOfInactiveUser_returns401() throws Exception {
        when(jwtService.parseUserId("good")).thenReturn(7L);
        when(userDetailsService.loadPrincipalById(7L)).thenReturn(principal(false, "ROLE_USER"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer good"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleComesFromDatabasePrincipal_notFromClient() throws Exception {
        when(jwtService.parseUserId("good")).thenReturn(7L);
        when(userDetailsService.loadPrincipalById(7L)).thenReturn(principal(true, "ROLE_USER"));

        // Aunque el cliente envie un header "role: ADMIN", sigue siendo USER.
        mockMvc.perform(post("/api/events").header("Authorization", "Bearer good").header("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(eventService);
    }

    @Test
    void principalPostProcessor_worksWithSecurityPrincipal() throws Exception {
        when(authService.me(any())).thenReturn(
                new AuthenticatedUserResponse(7L, "andrea", "andrea@mail.com", List.of("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").with(user(principal(true, "ROLE_USER"))))
                .andExpect(status().isOk());
    }
}
