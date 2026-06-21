package com.tecngo.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MetricsScrapeFilterTest {
    @Test
    void rejectsInvalidScrapeToken() throws Exception {
        MetricsScrapeFilter filter = new MetricsScrapeFilter("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/actuator/prometheus");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }

    @Test
    void acceptsBearerScrapeToken() throws Exception {
        MetricsScrapeFilter filter = new MetricsScrapeFilter("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/actuator/prometheus");
        request.addHeader("Authorization", "Bearer secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
