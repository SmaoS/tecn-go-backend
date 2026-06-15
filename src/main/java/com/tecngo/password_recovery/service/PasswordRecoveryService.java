package com.tecngo.password_recovery.service;

import com.tecngo.password_recovery.dto.PasswordMessageResponse;
import com.tecngo.password_recovery.dto.ResetPasswordRequest;
import com.tecngo.password_recovery.entity.PasswordResetToken;
import com.tecngo.password_recovery.entity.PasswordSecurityAudit;
import com.tecngo.password_recovery.repository.PasswordResetTokenRepository;
import com.tecngo.password_recovery.repository.PasswordSecurityAuditRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.verification.service.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryService {
    public static final String GENERIC_MESSAGE =
            "Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.";

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordSecurityAuditRepository audits;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${app.password-reset.url:${FRONTEND_URL:http://localhost:5173}/reset-password}")
    private String resetUrl;

    @Transactional
    public PasswordMessageResponse forgotPassword(String rawEmail) {
        users.findByEmailIgnoreCase(rawEmail.trim().toLowerCase()).ifPresent(this::createAndSend);
        return new PasswordMessageResponse(GENERIC_MESSAGE);
    }

    @Transactional
    public PasswordMessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        PasswordResetToken resetToken = tokens.findByToken(hash(request.token()))
                .orElseThrow(() -> new ConflictException("El enlace de recuperación no es válido o ya expiró"));
        if (resetToken.isUsed() || !resetToken.getExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("El enlace de recuperación no es válido o ya expiró");
        }
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        tokens.invalidateActiveByUserId(user.getId(), Instant.now());
        audits.save(PasswordSecurityAudit.builder().user(user).action("PASSWORD_RESET").build());
        return new PasswordMessageResponse("Contraseña actualizada correctamente.");
    }

    private void createAndSend(User user) {
        Instant now = Instant.now();
        tokens.invalidateActiveByUserId(user.getId(), now);
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(PasswordResetToken.builder()
                .user(user)
                .token(hash(rawToken))
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .build());
        audits.save(PasswordSecurityAudit.builder().user(user).action("PASSWORD_RESET_REQUESTED").build());
        try {
            emailSender.sendPasswordReset(user.getEmail(), user.getFullName(), resetUrl + "?token=" + rawToken);
        } catch (RuntimeException exception) {
            log.error("Password recovery email could not be sent to {}", user.getEmail(), exception);
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
