package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private SecurityPrincipal principal(boolean active, String... roles) {
        List<GrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
        return new SecurityPrincipal(7L, "andrea", "andrea@mail.com", "hash", active, authorities);
    }

    private MockFilterChain run(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    void noHeader_continuesWithoutAuthentication() throws Exception {
        MockFilterChain chain = run(new MockHttpServletRequest());

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validBearer_authenticatesWithCurrentPrincipalFromDatabase() throws Exception {
        when(jwtService.parseUserId("tok")).thenReturn(7L);
        when(userDetailsService.loadPrincipalById(7L)).thenReturn(principal(true, "ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");

        run(request);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(((SecurityPrincipal) auth.getPrincipal()).getUserId()).isEqualTo(7L);
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }

    @Test
    void invalidBearer_doesNotAuthenticateAndDoesNotFail() throws Exception {
        when(jwtService.parseUserId("bad")).thenThrow(new InvalidTokenException("bad"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");

        MockFilterChain chain = run(request);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void inactiveUser_doesNotAuthenticate() throws Exception {
        when(jwtService.parseUserId("tok")).thenReturn(7L);
        when(userDetailsService.loadPrincipalById(7L)).thenReturn(principal(false, "ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");

        run(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deletedUser_doesNotAuthenticate() throws Exception {
        when(jwtService.parseUserId("tok")).thenReturn(7L);
        when(userDetailsService.loadPrincipalById(7L)).thenThrow(new UsernameNotFoundException("gone"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");

        run(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void duplicatedAuthorizationHeader_isNotAuthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer a");
        request.addHeader("Authorization", "Bearer b");

        run(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.mockito.Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void nonBearerScheme_isIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        run(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.mockito.Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void tokenInQueryString_isNeverAccepted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("access_token", "tok");

        run(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.mockito.Mockito.verifyNoInteractions(jwtService);
    }

    @Test
    void existingAuthentication_isNotOverwritten() throws Exception {
        var existing = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated("someone", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(existing);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok");

        run(request);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        org.mockito.Mockito.verifyNoInteractions(jwtService);
    }
}
