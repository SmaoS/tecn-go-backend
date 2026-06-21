package com.tecngo.auth.ratelimit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface SecurityRateLimitRepository extends JpaRepository<SecurityRateLimitEvent, UUID> {
    long countByActionAndKeyHashAndCreatedAtAfter(String action, String keyHash, Instant after);
    void deleteByCreatedAtBefore(Instant cutoff);
}
