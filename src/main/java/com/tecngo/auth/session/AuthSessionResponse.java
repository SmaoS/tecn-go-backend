package com.tecngo.auth.session;

import java.time.Instant;
import java.util.UUID;

public record AuthSessionResponse(
        UUID id,
        Instant createdAt,
        Instant expiresAt,
        Instant lastSeenAt,
        Instant revokedAt,
        String userAgent,
        boolean current,
        boolean mfaVerified
) {
}
