package com.tecngo.auth.security;

import com.tecngo.auth.mfa.AdminMfaChallengeRepository;
import com.tecngo.auth.ratelimit.SecurityRateLimitRepository;
import com.tecngo.auth.session.AuthSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class SecurityDataCleanupJob {
    private final SecurityRateLimitRepository rateLimits;
    private final AdminMfaChallengeRepository challenges;
    private final AuthSessionRepository sessions;

    @Scheduled(fixedDelayString = "${app.parameters.data-clean-up:86400000}")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        rateLimits.deleteByCreatedAtBefore(now.minus(2, ChronoUnit.DAYS));
        challenges.deleteByExpiresAtBefore(now.minus(1, ChronoUnit.DAYS));
        sessions.deleteByExpiresAtBefore(now.minus(30, ChronoUnit.DAYS));
    }
}
