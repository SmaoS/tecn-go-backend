package com.tecngo.password_recovery.repository;

import com.tecngo.password_recovery.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("""
            update PasswordResetToken token
               set token.used = true, token.usedAt = :usedAt
             where token.user.id = :userId and token.used = false
            """)
    int invalidateActiveByUserId(@Param("userId") UUID userId, @Param("usedAt") Instant usedAt);
}
