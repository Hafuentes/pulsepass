package com.pulse.pass.service;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.security.SecurityPrincipal;

/**
 * Politicas de propiedad y visibilidad (anti-IDOR). Se invoca desde @PreAuthorize/@PostAuthorize/@PostFilter
 * como {@code @authz}. Compara SIEMPRE contra el ID persistente del principal autenticado, jamas contra
 * datos enviados por el cliente (SEC006).
 *
 * Politica de objeto ajeno: 403 de forma consistente (tambien cuando el recurso no existe, para
 * no revelar existencia a usuarios sin privilegios).
 */
@Service("authz")
public class AuthorizationService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public AuthorizationService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    // ---------- Tickets ----------

    /** Propietario, o STAFF/ADMIN (consulta operativa). */
    public boolean canAccessTicket(String ticketCode) {
        Optional<SecurityPrincipal> principal = currentPrincipal();
        if (principal.isEmpty()) {
            return false;
        }
        if (isStaffOrAdmin(principal.get())) {
            return true;
        }
        return isTicketOwner(principal.get(), ticketCode);
    }

    /** Propietario, o ADMIN expresamente autorizado. */
    public boolean canCancelTicket(String ticketCode) {
        Optional<SecurityPrincipal> principal = currentPrincipal();
        if (principal.isEmpty()) {
            return false;
        }
        return principal.get().hasRole("ADMIN") || isTicketOwner(principal.get(), ticketCode);
    }

    /** El userEmail del DTO debe pertenecer al principal (ID persistente). */
    public boolean canPurchaseFor(String userEmail) {
        return isSelfByEmail(userEmail);
    }

    /** Tickets de un usuario: el propio usuario, o ADMIN. */
    public boolean canListTicketsOf(String email) {
        return isAdmin() || isSelfByEmail(email);
    }

    // ---------- Usuarios ----------

    public boolean canAccessUserByEmail(String email) {
        return isAdmin() || isSelfByEmail(email);
    }

    public boolean canAccessUserByUsername(String username) {
        Optional<SecurityPrincipal> principal = currentPrincipal();
        if (principal.isEmpty() || username == null) {
            return false;
        }
        if (principal.get().hasRole("ADMIN")) {
            return true;
        }
        return userRepository.findByUsername(username)
                .map(u -> u.getId().equals(principal.get().getUserId()))
                .orElse(false);
    }

    // ---------- Eventos (visibilidad de borradores) ----------

    /** Publicos: solo PUBLISHED. STAFF/ADMIN ven cualquier estado. */
    public boolean canViewEvent(EventResponse event) {
        return event == null || event.status() == EventStatus.PUBLISHED || isStaffOrAdminCurrent();
    }

    public boolean canViewEventSummary(EventSummaryResponse event) {
        return event == null || event.status() == EventStatus.PUBLISHED || isStaffOrAdminCurrent();
    }

    // ---------- Utilidades ----------

    public Optional<SecurityPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof SecurityPrincipal p ? Optional.of(p) : Optional.empty();
    }

    private boolean isTicketOwner(SecurityPrincipal principal, String ticketCode) {
        if (ticketCode == null) {
            return false;
        }
        return ticketRepository.findOwnerIdByTicketCode(ticketCode)
                .map(ownerId -> ownerId.equals(principal.getUserId()))
                .orElse(false);
    }

    private boolean isSelfByEmail(String email) {
        Optional<SecurityPrincipal> principal = currentPrincipal();
        if (principal.isEmpty() || email == null) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(email)
                .map(u -> u.getId().equals(principal.get().getUserId()))
                .orElse(false);
    }

    private boolean isAdmin() {
        return currentPrincipal().map(p -> p.hasRole("ADMIN")).orElse(false);
    }

    private boolean isStaffOrAdminCurrent() {
        return currentPrincipal().map(this::isStaffOrAdmin).orElse(false);
    }

    private boolean isStaffOrAdmin(SecurityPrincipal principal) {
        return principal.hasRole("STAFF") || principal.hasRole("ADMIN");
    }
}
