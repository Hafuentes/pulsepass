package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

/** NFR-SEC006: cada request recibe un requestId trazable (MDC + cabecera) y el MDC se limpia al final. */
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    private String run(MockHttpServletRequest request, MockHttpServletResponse response,
                       AtomicReference<String> seenInMdc) throws Exception {
        FilterChain chain = (req, res) -> seenInMdc.set(MDC.get(RequestIdFilter.MDC_KEY));
        filter.doFilter(request, response, chain);
        return response.getHeader(RequestIdFilter.HEADER);
    }

    @Test
    void withoutHeader_generatesUuid_exposesItInMdcAndResponse_andClearsMdc() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();

        String header = run(new MockHttpServletRequest("GET", "/api/venues/active"),
                new MockHttpServletResponse(), seen);

        assertThat(header).isNotBlank().matches("^[0-9a-f-]{36}$");
        assertThat(seen.get()).isEqualTo(header);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void safeIncomingId_isReused() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/venues/active");
        request.addHeader(RequestIdFilter.HEADER, "client-req-12345");
        AtomicReference<String> seen = new AtomicReference<>();

        String header = run(request, new MockHttpServletResponse(), seen);

        assertThat(header).isEqualTo("client-req-12345");
        assertThat(seen.get()).isEqualTo("client-req-12345");
    }

    @Test
    void unsafeIncomingId_isReplaced_toPreventLogInjection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/venues/active");
        request.addHeader(RequestIdFilter.HEADER, "abc\nFAKE LOG LINE injected");
        AtomicReference<String> seen = new AtomicReference<>();

        String header = run(request, new MockHttpServletResponse(), seen);

        assertThat(header).doesNotContain("FAKE").doesNotContain("\n").matches("^[0-9a-f-]{36}$");
        assertThat(seen.get()).isEqualTo(header);
    }

    @Test
    void mdcIsClearedEvenWhenTheChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/venues/active");
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(request, new MockHttpServletResponse(), failing);
        } catch (Exception expected) {
            // esperado
        }

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
