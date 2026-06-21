package com.tecngo.auth.mfa;

import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.TooManyRequestsException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.verification.service.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AdminMfaService {
    private final AdminMfaChallengeRepository challenges;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.security.admin-mfa-enabled:true}")
    private boolean enabled;

    @Value("${app.security.admin-mfa-expiration-minutes:10}")
    private long expirationMinutes;

    public boolean required(User user) {
        return enabled && (user.hasRole(Role.ADMIN) || user.hasRole(Role.VERIFIER));
    }

    @Transactional
    public Challenge challenge(User user, String clientIp) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ConflictException("Administrative MFA requires a registered email");
        }
        Instant window = Instant.now().minus(15, ChronoUnit.MINUTES);
        if (challenges.countByUserIdAndCreatedAtAfter(user.getId(), window) >= 5) {
            throw new TooManyRequestsException("MFA_RATE_LIMITED",
                    "Too many MFA codes requested. Try again later.");
        }
        String code = "%06d".formatted(random.nextInt(1_000_000));
        String rawToken = token();
        Instant expiresAt = Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
        challenges.save(AdminMfaChallenge.builder()
                .user(user)
                .challengeTokenHash(hash(rawToken))
                .codeHash(passwordEncoder.encode(code))
                .requestIpHash(hash(clientIp))
                .expiresAt(expiresAt)
                .build());
        emailSender.sendMfaCode(user.getEmail(), user.getFullName(), code, expirationMinutes);
        return new Challenge(rawToken, expiresAt);
    }

    @Transactional(noRollbackFor = ConflictException.class)
    public User verify(String rawToken, String code) {
        AdminMfaChallenge challenge = challenges.findByChallengeTokenHash(hash(rawToken))
                .orElseThrow(() -> new ConflictException("MFA challenge is invalid or expired"));
        if (challenge.getConsumedAt() != null || !challenge.getExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("MFA challenge is invalid or expired");
        }
        if (challenge.getAttempts() >= 5) {
            throw new ConflictException("Maximum MFA verification attempts reached");
        }
        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenges.save(challenge);
            throw new ConflictException("MFA code is invalid or expired");
        }
        challenge.setConsumedAt(Instant.now());
        return challenge.getUser();
    }

    private String token() {
        byte[] value = new byte[32];
        random.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash MFA token", exception);
        }
    }

    public record Challenge(String token, Instant expiresAt) {
    }
}
