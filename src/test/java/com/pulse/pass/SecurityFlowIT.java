package com.pulse.pass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Role;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import com.pulse.pass.repository.VenueRepository;

/**
 * Integracion de seguridad contra PostgreSQL real (Flyway V4, BCrypt, JWT real, anti-IDOR, estado de usuario).
 * Usa contrasenas y secretos ficticios exclusivos de pruebas.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SecurityFlowIT {

    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCredentialRepository credentialRepository;
    @Autowired
    private UserRoleRepository roleRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String suffix;
    private String eventCode;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        suffix = UUID.randomUUID().toString().substring(0, 8);
        Venue venue = venueRepository.save(new Venue("V-" + suffix, "Venue " + suffix, "Santa Marta", "x", 100, true));
        eventCode = "EV-" + suffix;
        eventRepository.save(new Event(eventCode, "Fest " + suffix, "d", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), 0, null, venue));
    }

    // ---------- helpers ----------

    private String registerBody(String username) {
        return """
                {"username":"%1$s","email":"%1$s@mail.com","firstName":"N","lastName":"L",
                 "birthDate":"1995-05-20","password":"%2$s"}
                """.formatted(username, PASSWORD);
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(registerBody(username)))
                .andExpect(status().isCreated());
    }

    private String login(String identifier, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().doesNotExist("Set-Cookie")) // FR-SECAUTH-008: sin JSESSIONID
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    /** Alta controlada de cuentas privilegiadas (nunca por HTTP). */
    private void provisionPrivileged(String username, Role... roles) {
        User user = userRepository.save(new User(username, username + "@mail.com", true));
        credentialRepository.save(new UserCredential(user.getId(), passwordEncoder.encode(PASSWORD), LocalDateTime.now()));
        for (Role role : roles) {
            roleRepository.save(new UserRole(user.getId(), role));
        }
    }

    private String buyTicket(String token, String email) throws Exception {
        String body = mockMvc.perform(post("/api/tickets").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"%s\",\"eventCode\":\"%s\",\"type\":\"GENERAL\"}"
                                .formatted(email, eventCode)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.ticketCode");
    }

    // ---------- registro y login ----------

    @Test
    void register_storesBcryptHashAndUserRole_withoutLeakingSecrets() throws Exception {
        String username = "andrea" + suffix;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(registerBody(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User user = userRepository.findByUsername(username).orElseThrow();
        UserCredential credential = credentialRepository.findById(user.getId()).orElseThrow();
        assertThat(credential.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(roleRepository.findByUserId(user.getId())).extracting(UserRole::getRole).containsExactly(Role.USER);
    }

    @Test
    void register_duplicate_createsNoSecondCredential_andReturns409() throws Exception {
        String username = "dup" + suffix;
        register(username);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(registerBody(username)))
                .andExpect(status().isConflict());
        assertThat(userRepository.findByUsername(username)).isPresent();
    }

    @Test
    void login_byUsernameAndByEmail_andMe() throws Exception {
        String username = "andrea" + suffix;
        register(username);

        String byUsername = login(username, PASSWORD);
        String byEmail = login(username + "@mail.com", PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(byUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(byEmail).isNotEqualTo(byUsername); // jti distinto
    }

    @Test
    void login_wrongPasswordAndUnknownUser_returnSameGeneric401() throws Exception {
        String username = "andrea" + suffix;
        register(username);

        String wrong = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"incorrecta-123456\"}".formatted(username)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknown = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ghost%s\",\"password\":\"incorrecta-123456\"}".formatted(suffix)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(wrong, "$.message")).isEqualTo(JsonPath.read(unknown, "$.message"));
        assertThat(wrong).doesNotContain("$2").doesNotContainIgnoringCase("exception");
    }

    @Test
    void legacyUserWithoutCredentials_cannotLogin() throws Exception {
        userRepository.save(new User("legacy" + suffix, "legacy" + suffix + "@mail.com", true));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"legacy%s\",\"password\":\"%s\"}".formatted(suffix, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- anti-IDOR ----------

    @Test
    void user_cannotPurchaseForAnotherUser_orReadOrCancelForeignTickets() throws Exception {
        String andrea = "andrea" + suffix;
        String carlos = "carlos" + suffix;
        register(andrea);
        register(carlos);
        String andreaToken = login(andrea, PASSWORD);
        String carlosToken = login(carlos, PASSWORD);

        // UC-SEC-05: compra para otro -> 403
        mockMvc.perform(post("/api/tickets").header("Authorization", bearer(andreaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"%s@mail.com\",\"eventCode\":\"%s\",\"type\":\"GENERAL\"}"
                                .formatted(carlos, eventCode)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/by-user").param("email", carlos + "@mail.com")
                        .header("Authorization", bearer(carlosToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        // compra propia -> 201; Carlos compra la suya
        String andreaTicket = buyTicket(andreaToken, andrea + "@mail.com");
        String carlosTicket = buyTicket(carlosToken, carlos + "@mail.com");

        // propietario ve el suyo
        mockMvc.perform(get("/api/tickets/" + andreaTicket).header("Authorization", bearer(andreaToken)))
                .andExpect(status().isOk());
        // UC-SEC-06: ticket ajeno -> 403 (consulta, listado, cancelacion, uso)
        mockMvc.perform(get("/api/tickets/" + carlosTicket).header("Authorization", bearer(andreaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/by-user").param("email", carlos + "@mail.com")
                        .header("Authorization", bearer(andreaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/" + carlosTicket + "/cancel").header("Authorization", bearer(andreaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/" + andreaTicket + "/use").header("Authorization", bearer(andreaToken)))
                .andExpect(status().isForbidden());
        // ticket inexistente -> 403 tambien (sin oraculo de existencia)
        mockMvc.perform(get("/api/tickets/NOPE-" + suffix).header("Authorization", bearer(andreaToken)))
                .andExpect(status().isForbidden());
        // el ticket de Carlos sigue PAID
        mockMvc.perform(get("/api/tickets/" + carlosTicket).header("Authorization", bearer(carlosToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        // propietario puede cancelar el suyo (regla del Service)
        mockMvc.perform(patch("/api/tickets/" + andreaTicket + "/cancel").header("Authorization", bearer(andreaToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void user_cannotLookUpOtherUsers_butCanLookUpSelf() throws Exception {
        String andrea = "andrea" + suffix;
        String carlos = "carlos" + suffix;
        register(andrea);
        register(carlos);
        String token = login(andrea, PASSWORD);

        mockMvc.perform(get("/api/users/by-username").param("username", andrea).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/by-email").param("email", andrea + "@mail.com").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/by-username").param("username", carlos).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-email").param("email", carlos + "@mail.com").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    // ---------- roles ----------

    @Test
    void roles_userCannotPublish_adminCan_staffUsesTicketOnce() throws Exception {
        String andrea = "andrea" + suffix;
        register(andrea);
        provisionPrivileged("admin" + suffix, Role.ADMIN);
        provisionPrivileged("laura" + suffix, Role.STAFF);
        String userToken = login(andrea, PASSWORD);
        String adminToken = login("admin" + suffix, PASSWORD);
        String staffToken = login("laura" + suffix, PASSWORD);

        // evento en DRAFT: UC-SEC-08 y visibilidad
        Venue venue = venueRepository.findByCode("V-" + suffix).orElseThrow();
        String draft = "DR-" + suffix;
        eventRepository.save(new Event(draft, "Draft", "d", EventCategory.MUSIC, EventStatus.DRAFT,
                LocalDateTime.now().plusDays(30), 0, null, venue));

        mockMvc.perform(get("/api/events/" + draft)).andExpect(status().isUnauthorized());          // anonimo
        mockMvc.perform(get("/api/events/" + draft).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());                                                  // USER
        mockMvc.perform(get("/api/events/" + draft).header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk());                                                         // STAFF
        mockMvc.perform(get("/api/events/" + eventCode)).andExpect(status().isOk());                 // publicado

        mockMvc.perform(patch("/api/events/" + draft + "/publish").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/events/" + draft + "/publish").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));

        // UC-SEC-07: STAFF marca uso; segundo uso -> 409 por regla heredada
        String ticket = buyTicket(userToken, andrea + "@mail.com");
        mockMvc.perform(patch("/api/tickets/" + ticket + "/use").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/" + ticket + "/use").header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("USED"));
        mockMvc.perform(patch("/api/tickets/" + ticket + "/use").header("Authorization", bearer(staffToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/events/" + eventCode + "/tickets/paid").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void byArtist_hidesDraftsFromAnonymousAndUser_butShowsThemToStaff() throws Exception {
        // FR-SECEVT-06 / T-SEC-017: un artista con un evento publicado y otro en borrador.
        Artist artist = artistRepository.saveAndFlush(new Artist("Artist " + suffix, "Colombia", "Electronic", true));
        Venue venue = venueRepository.findByCode("V-" + suffix).orElseThrow();
        String published = "PUB-" + suffix;
        String draft = "DRA-" + suffix;
        eventRepository.save(new Event(published, "Published", "d", EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(30), 0, null, venue));
        eventRepository.save(new Event(draft, "Draft", "d", EventCategory.MUSIC, EventStatus.DRAFT,
                LocalDateTime.now().plusDays(30), 0, null, venue));

        // El ADMIN asocia al artista a ambos eventos por la ruta real.
        provisionPrivileged("admin" + suffix, Role.ADMIN);
        String adminToken = login("admin" + suffix, PASSWORD);
        for (String code : List.of(published, draft)) {
            mockMvc.perform(post("/api/events/" + code + "/artists/" + artist.getId())
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk());
        }

        String andrea = "andrea" + suffix;
        register(andrea);
        provisionPrivileged("laura" + suffix, Role.STAFF);
        String userToken = login(andrea, PASSWORD);
        String staffToken = login("laura" + suffix, PASSWORD);

        // anonimo: solo el publicado
        List<String> anonymous = eventCodesByArtist("Artist " + suffix, null);
        assertThat(anonymous).contains(published).doesNotContain(draft);
        // USER autenticado: igual que el anonimo
        assertThat(eventCodesByArtist("Artist " + suffix, userToken)).contains(published).doesNotContain(draft);
        // STAFF: ve ambos
        assertThat(eventCodesByArtist("Artist " + suffix, staffToken)).contains(published, draft);
    }

    private List<String> eventCodesByArtist(String stageName, String token) throws Exception {
        var request = get("/api/events/by-artist").param("stageName", stageName);
        if (token != null) {
            request = request.header("Authorization", bearer(token));
        }
        String body = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].eventCode");
    }

    // ---------- JWT ----------

    @Test
    void tamperedToken_returns401() throws Exception {
        String andrea = "andrea" + suffix;
        register(andrea);
        String token = login(andrea, PASSWORD);
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

        String body = mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();

        // T-SEC-019: sin stacktrace, sin el token y sin detalles internos
        assertThat(body).doesNotContain(tampered).doesNotContain("at com.")
                .doesNotContainIgnoringCase("exception").doesNotContainIgnoringCase("jwt");
    }

    @Test
    void deactivatedUser_losesAccessWithStillValidToken() throws Exception {
        String andrea = "andrea" + suffix;
        register(andrea);
        String token = login(andrea, PASSWORD);
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token))).andExpect(status().isOk());

        User user = userRepository.findByUsername(andrea).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(andrea, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedRole_takesEffectOnNextRequest() throws Exception {
        provisionPrivileged("admin" + suffix, Role.ADMIN);
        String adminToken = login("admin" + suffix, PASSWORD);
        String draft = "DR2-" + suffix;
        Venue venue = venueRepository.findByCode("V-" + suffix).orElseThrow();
        eventRepository.save(new Event(draft, "Draft", "d", EventCategory.MUSIC, EventStatus.DRAFT,
                LocalDateTime.now().plusDays(30), 0, null, venue));

        User admin = userRepository.findByUsername("admin" + suffix).orElseThrow();
        roleRepository.deleteById(new UserRole.Key(admin.getId(), Role.ADMIN));
        roleRepository.save(new UserRole(admin.getId(), Role.USER));

        mockMvc.perform(patch("/api/events/" + draft + "/publish").header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
    }
}
