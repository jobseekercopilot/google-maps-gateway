package com.jobseekercopilot.googlemapsgateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalServiceTokenFilterTest {
    private static final String TOKEN = "google-maps-gateway-token-32-bytes-minimum";

    @Test
    void rejectsAnUnsafeConfiguredToken() {
        assertThatThrownBy(() -> new InternalServiceTokenFilter("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingCallerToken() throws Exception {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/routes/matrix");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void acceptsTheConfiguredCallerToken() throws Exception {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/routes/matrix");
        request.addHeader(InternalServiceTokenFilter.HEADER, TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void leavesActuatorHealthAvailableWithoutAServiceToken() throws Exception {
        InternalServiceTokenFilter filter = new InternalServiceTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
