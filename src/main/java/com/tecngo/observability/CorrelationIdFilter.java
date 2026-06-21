package com.tecngo.observability;

import io.sentry.Sentry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{8,100}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = valid(request.getHeader(HEADER))
                ? request.getHeader(HEADER)
                : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, correlationId);
        request.setAttribute(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        Sentry.configureScope(scope -> scope.setTag(MDC_KEY, correlationId));
        try {
            filterChain.doFilter(request, response);
        } finally {
            Sentry.configureScope(scope -> scope.removeTag(MDC_KEY));
            MDC.remove(MDC_KEY);
        }
    }

    private boolean valid(String value) {
        return value != null && SAFE_ID.matcher(value).matches();
    }
}
