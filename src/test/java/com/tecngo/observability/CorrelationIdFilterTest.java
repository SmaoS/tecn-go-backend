package com.tecngo.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesValidClientCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/services");
        request.addHeader(CorrelationIdFilter.HEADER, "client-request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("client-request-123");
        assertThat(request.getAttribute(CorrelationIdFilter.MDC_KEY)).isEqualTo("client-request-123");
    }

    @Test
    void replacesUnsafeCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/services");
        request.addHeader(CorrelationIdFilter.HEADER, "unsafe id with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isNotBlank()
                .isNotEqualTo("unsafe id with spaces");
    }
}
