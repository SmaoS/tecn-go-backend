package com.tecngo.auth.ratelimit;

import com.tecngo.shared.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class SecurityRateLimitService {
    private final SecurityRateLimitRepository events;

    @Transactional(readOnly = true)
    public void check(String action, String key, int limit, Duration window) {
        if (events.countByActionAndKeyHashAndCreatedAtAfter(
                action, hash(key), Instant.now().minus(window)) >= limit) {
            throw new TooManyRequestsException("AUTH_RATE_LIMITED",
                    "Too many requests. Try again later.");
        }
    }

    @Transactional
    public void record(String action, String key) {
        events.save(SecurityRateLimitEvent.builder()
                .action(action)
                .keyHash(hash(key))
                .build());
    }

    private String hash(String value) {
        String normalized = value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash rate limit key", exception);
        }
    }
}
