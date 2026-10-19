package com.pulse.pass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.User;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.security.SecurityPrincipal;

class AuthorizationServiceTest {

    private static final long ANDREA = 1L;
    private static final long CARLOS = 2L;

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthorizationService authz = new AuthorizationService(ticketRepository, userRepository);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long id, String... roles) {
        List<GrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        SecurityPrincipal principal = new SecurityPrincipal(id, "u" + id, "u" + id + "@mail.com", "h", true, authorities);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
    }

    private User userWithId(long id) throws Exception {
        User user = new User("u" + id, "u" + id + "@mail.com", true);
        Field f = User.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(user, id);
        return user;
    }

    // ---- tickets ----

    @Test
    void owner_canAccessAndCancelOwnTicket() {
        loginAs(ANDREA, "USER");
        when(ticketRepository.findOwnerIdByTicketCode("T1")).thenReturn(Optional.of(ANDREA));

        assertThat(authz.canAccessTicket("T1")).isTrue();
        assertThat(authz.canCancelTicket("T1")).isTrue();
    }

    @Test
    void user_cannotAccessOrCancelAnotherUsersTicket() {
        loginAs(ANDREA, "USER");
        when(ticketRepository.findOwnerIdByTicketCode("T2")).thenReturn(Optional.of(CARLOS));

        assertThat(authz.canAccessTicket("T2")).isFalse();
        assertThat(authz.canCancelTicket("T2")).isFalse();
    }

    @Test
    void user_nonexistentTicket_isDenied_noExistenceOracle() {
        loginAs(ANDREA, "USER");
        when(ticketRepository.findOwnerIdByTicketCode("NOPE")).thenReturn(Optional.empty());

        assertThat(authz.canAccessTicket("NOPE")).isFalse();
    }

    @Test
    void staff_canViewButNotCancelForeignTicket() {
        loginAs(99L, "STAFF");
        when(ticketRepository.findOwnerIdByTicketCode("T2")).thenReturn(Optional.of(CARLOS));

        assertThat(authz.canAccessTicket("T2")).isTrue();
        assertThat(authz.canCancelTicket("T2")).isFalse();
    }

    @Test
    void admin_canViewAndCancelForeignTicket() {
        loginAs(100L, "ADMIN");
        when(ticketRepository.findOwnerIdByTicketCode("T2")).thenReturn(Optional.of(CARLOS));

        assertThat(authz.canAccessTicket("T2")).isTrue();
        assertThat(authz.canCancelTicket("T2")).isTrue();
    }

    @Test
    void purchase_onlyForSelf_byPersistentId() throws Exception {
        loginAs(ANDREA, "USER");
        when(userRepository.findByEmailIgnoreCase("u1@mail.com")).thenReturn(Optional.of(userWithId(ANDREA)));
        when(userRepository.findByEmailIgnoreCase("u2@mail.com")).thenReturn(Optional.of(userWithId(CARLOS)));
        when(userRepository.findByEmailIgnoreCase("ghost@mail.com")).thenReturn(Optional.empty());

        assertThat(authz.canPurchaseFor("u1@mail.com")).isTrue();
        assertThat(authz.canPurchaseFor("u2@mail.com")).isFalse();
        assertThat(authz.canPurchaseFor("ghost@mail.com")).isFalse();
        assertThat(authz.canPurchaseFor(null)).isFalse();
    }

    @Test
    void listTickets_selfOrAdminOnly() throws Exception {
        when(userRepository.findByEmailIgnoreCase("u2@mail.com")).thenReturn(Optional.of(userWithId(CARLOS)));

        loginAs(ANDREA, "USER");
        assertThat(authz.canListTicketsOf("u2@mail.com")).isFalse();

        loginAs(99L, "STAFF");
        assertThat(authz.canListTicketsOf("u2@mail.com")).isFalse();

        loginAs(100L, "ADMIN");
        assertThat(authz.canListTicketsOf("u2@mail.com")).isTrue();
    }

    // ---- usuarios ----

    @Test
    void userLookups_selfOrAdminOnly() throws Exception {
        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(userWithId(ANDREA)));
        when(userRepository.findByUsername("u2")).thenReturn(Optional.of(userWithId(CARLOS)));

        loginAs(ANDREA, "USER");
        assertThat(authz.canAccessUserByUsername("u1")).isTrue();
        assertThat(authz.canAccessUserByUsername("u2")).isFalse();
        assertThat(authz.canAccessUserByUsername("ghost")).isFalse();

        loginAs(100L, "ADMIN");
        assertThat(authz.canAccessUserByUsername("u2")).isTrue();
    }

    // ---- anonimo ----

    @Test
    void anonymous_isAlwaysDenied() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(authz.canAccessTicket("T1")).isFalse();
        assertThat(authz.canCancelTicket("T1")).isFalse();
        assertThat(authz.canPurchaseFor("u1@mail.com")).isFalse();
        assertThat(authz.canAccessUserByEmail("u1@mail.com")).isFalse();
    }

    @Test
    void noAuthentication_isDenied() {
        assertThat(authz.canAccessTicket("T1")).isFalse();
        assertThat(authz.currentPrincipal()).isEmpty();
    }

    // ---- visibilidad de eventos ----

    private EventResponse event(EventStatus status) {
        return new EventResponse(1L, "E1", "n", "d", EventCategory.MUSIC, status,
                LocalDateTime.now().plusDays(5), 0, "V", "Venue", List.of());
    }

    private EventSummaryResponse summary(EventStatus status) {
        return new EventSummaryResponse(1L, "E1", "n", EventCategory.MUSIC, status,
                LocalDateTime.now().plusDays(5), "V", "Venue", "City");
    }

    @Test
    void anonymous_seesOnlyPublishedEvents() {
        assertThat(authz.canViewEvent(event(EventStatus.PUBLISHED))).isTrue();
        assertThat(authz.canViewEvent(event(EventStatus.DRAFT))).isFalse();
        assertThat(authz.canViewEventSummary(summary(EventStatus.DRAFT))).isFalse();
    }

    @Test
    void user_doesNotSeeDrafts_staffAndAdminDo() {
        loginAs(ANDREA, "USER");
        assertThat(authz.canViewEvent(event(EventStatus.DRAFT))).isFalse();

        loginAs(99L, "STAFF");
        assertThat(authz.canViewEvent(event(EventStatus.DRAFT))).isTrue();

        loginAs(100L, "ADMIN");
        assertThat(authz.canViewEventSummary(summary(EventStatus.DRAFT))).isTrue();
    }
}
