package com.pulse.pass.security;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee Authorization: Bearer, valida el JWT y construye el contexto con el principal vigente de BD.
 * Si falta el header o el token no es valido simplemente NO autentica: la cadena decide
 * (ruta publica -> continua; ruta protegida -> AuthenticationEntryPoint 401). Nunca produce 500.
 *
 * No es un @Component a proposito: lo construye SecurityConfig para que el contenedor de servlets
 * no lo registre por segunda vez fuera de la cadena de Spring Security.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractBearerToken(request);

        // No se sobrescribe una autenticacion ya establecida.
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = jwtService.parseUserId(token);
                SecurityPrincipal principal = userDetailsService.loadPrincipalById(userId);
                if (principal.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
                            .authenticated(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                } else {
                    // SEC011: usuario desactivado pierde acceso en el siguiente request.
                    log.warn("Access denied: inactive user id={}", principal.getUserId());
                }
            } catch (InvalidTokenException | UsernameNotFoundException ex) {
                // Jamas se loguea el token.
                log.debug("Bearer token rejected: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    /** Devuelve el token o null si no hay un unico header Authorization con esquema Bearer. */
    private static String extractBearerToken(HttpServletRequest request) {
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.size() != 1) {
            return null; // ausente o duplicado (sospechoso): no se autentica.
        }
        String header = headers.get(0);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
