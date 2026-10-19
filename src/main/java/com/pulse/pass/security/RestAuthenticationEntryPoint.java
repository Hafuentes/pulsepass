package com.pulse.pass.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 401 sin credenciales validas. Delega en el HandlerExceptionResolver para que
 * GlobalExceptionHandler emita el mismo ErrorResponse (una sola escritura de respuesta).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);

    private final HandlerExceptionResolver resolver;

    public RestAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // NFR-SEC006: denegacion trazable (el requestId sale por el MDC); jamas se loguea el token.
        log.warn("Unauthenticated request denied: {} {}", request.getMethod(), request.getRequestURI());
        response.setHeader("WWW-Authenticate", "Bearer");
        resolver.resolveException(request, response, null, authException);
    }
}
