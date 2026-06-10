package com.tecngo.verification.service;

import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.verification.entity.VerificationToken;
import com.tecngo.verification.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    private final VerificationTokenRepository tokens;
    private final UserRepository users;
    private final EmailSender emailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.email.verification-expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${app.email.verification-url:http://localhost:5173/verificar-correo}")
    private String verificationUrl;

    @Value("${app.email.require-verification:false}")
    private boolean requireVerification;

    @Transactional
    public void send(User user) {
        if (user.isEmailVerified()) {
            log.info("Verification email skipped for {} because the address is already verified", user.getEmail());
            return;
        }
        log.info("Generating email verification token for {}", user.getEmail());
        tokens.deleteByUserId(user.getId());
        String rawToken = token();
        tokens.save(VerificationToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)))
                .build());
        emailSender.sendVerification(user.getEmail(), user.getFullName(),
                verificationUrl + (verificationUrl.contains("?") ? "&" : "?") + "token=" + rawToken);
    }

    @Transactional
    public User verify(String rawToken) {
        VerificationToken token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new NotFoundException("Verification token not found"));
        if (token.getUsedAt() != null) throw new ConflictException("Verification token was already used");
        if (token.getExpiresAt().isBefore(Instant.now())) throw new ConflictException("Verification token expired");
        token.setUsedAt(Instant.now());
        User user = token.getUser();
        user.setEmailVerified(true);
        return users.save(user);
    }

    public void requireVerified(User user) {
        if (requireVerification && !user.isEmailVerified()) {
            throw new ConflictException("Verify your email before continuing");
        }
    }

    private String token() {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash verification token", exception);
        }
    }
}
