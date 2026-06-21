package com.tecngo.auth.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    List<AuthSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("""
            update AuthSession session
               set session.revokedAt = :now, session.revokeReason = :reason
             where session.user.id = :userId and session.revokedAt is null
            """)
    int revokeAll(@Param("userId") UUID userId, @Param("now") Instant now,
                  @Param("reason") String reason);

    void deleteByExpiresAtBefore(Instant cutoff);
}
