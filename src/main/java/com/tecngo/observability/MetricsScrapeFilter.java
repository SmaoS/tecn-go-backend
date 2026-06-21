package com.tecngo.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MetricsScrapeFilter extends OncePerRequestFilter {
    private final String token;

    public MetricsScrapeFilter(@Value("${app.observability.metrics-scrape-token:}") String token) {
        this.token = token == null ? "" : token.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().endsWith("/actuator/prometheus");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!token.isBlank()) {
            String authorization = request.getHeader("Authorization");
            String received = authorization != null && authorization.startsWith("Bearer ")
                    ? authorization.substring(7) : "";
            if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                    received.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
