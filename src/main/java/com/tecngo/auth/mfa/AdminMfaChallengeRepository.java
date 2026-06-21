package com.tecngo.auth.mfa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AdminMfaChallengeRepository extends JpaRepository<AdminMfaChallenge, UUID> {
    Optional<AdminMfaChallenge> findByChallengeTokenHash(String challengeTokenHash);
    long countByUserIdAndCreatedAtAfter(UUID userId, Instant after);
    void deleteByExpiresAtBefore(Instant cutoff);
}
