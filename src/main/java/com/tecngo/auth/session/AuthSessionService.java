package com.tecngo.auth.session;

import com.tecngo.auth.service.JwtService;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthSessionService {
    private final AuthSessionRepository sessions;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.security.require-persisted-sessions:true}")
    private boolean requirePersistedSessions;

    @Transactional
    public String issue(User user, String clientIp, String userAgent, boolean mfaVerified) {
        Instant now = Instant.now();
        AuthSession session = sessions.save(AuthSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .expiresAt(now.plusMillis(expirationMs))
                .ipHash(hash(clientIp))
                .userAgent(clean(userAgent, 500))
                .mfaVerified(mfaVerified)
                .build());
        return jwtService.generateToken(user, session.getId(), session.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public boolean isActive(UUID sessionId, UUID userId) {
        if (!requirePersistedSessions) return true;
        if (sessionId == null) return false;
        return sessions.findById(sessionId)
                .filter(session -> session.getUser().getId().equals(userId))
                .filter(session -> session.getRevokedAt() == null)
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public String renewToken(User user, UUID sessionId) {
        AuthSession session = requireOwned(sessionId, user);
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(Instant.now())) {
            throw new ForbiddenException("Authentication session is no longer active");
        }
        return jwtService.generateToken(user, session.getId(), session.getExpiresAt());
    }

    @Transactional
    public void touch(UUID sessionId) {
        if (sessionId == null) return;
        sessions.findById(sessionId).ifPresent(session -> {
            Instant now = Instant.now();
            if (session.getLastSeenAt() == null
                    || session.getLastSeenAt().isBefore(now.minusSeconds(300))) {
                session.setLastSeenAt(now);
            }
        });
    }

    @Transactional
    public void revoke(UUID sessionId, User user, String reason) {
        AuthSession session = requireOwned(sessionId, user);
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(Instant.now());
            session.setRevokeReason(clean(reason, 120));
        }
    }

    @Transactional
    public void revokeAll(UUID userId, String reason) {
        sessions.revokeAll(userId, Instant.now(), clean(reason, 120));
    }

    @Transactional(readOnly = true)
    public List<AuthSessionResponse> mine(User user, UUID currentSessionId) {
        return sessions.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(session -> new AuthSessionResponse(
                        session.getId(), session.getCreatedAt(), session.getExpiresAt(),
                        session.getLastSeenAt(), session.getRevokedAt(), session.getUserAgent(),
                        session.getId().equals(currentSessionId), session.isMfaVerified()))
                .toList();
    }

    private AuthSession requireOwned(UUID sessionId, User user) {
        return sessions.findById(sessionId)
                .filter(session -> session.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Authentication session not found"));
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash session metadata", exception);
        }
    }

    private String clean(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }
}
