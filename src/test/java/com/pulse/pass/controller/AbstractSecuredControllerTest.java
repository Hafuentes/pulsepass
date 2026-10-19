package com.pulse.pass.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.pulse.pass.security.CustomUserDetailsService;
import com.pulse.pass.security.JwtService;
import com.pulse.pass.service.AuthorizationService;

/**
 * Base de las pruebas @WebMvcTest con la cadena de seguridad REAL (SecurityConfig + 401/403 JSON).
 * Las subclases deben anotar @Import({SecurityConfig.class, RestAuthenticationEntryPoint.class,
 * RestAccessDeniedHandler.class}). No se usa addFilters=false: el objetivo es probar la seguridad.
 */
abstract class AbstractSecuredControllerTest {

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mockMvc;

    /**
     * MockMvc con springSecurity() explicito: asi @WithMockUser y los post-processors de spring-security-test
     * se aplican sobre la cadena de filtros REAL (no se usa addFilters=false).
     */
    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected CustomUserDetailsService userDetailsService;

    /** Bean "authz" referenciado por @PreAuthorize/@PostAuthorize. */
    @MockitoBean(name = "authz")
    protected AuthorizationService authz;
}
