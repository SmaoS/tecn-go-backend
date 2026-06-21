package com.tecngo.password_recovery;

import com.tecngo.password_recovery.dto.ResetPasswordRequest;
import com.tecngo.password_recovery.entity.PasswordResetToken;
import com.tecngo.password_recovery.repository.PasswordResetTokenRepository;
import com.tecngo.password_recovery.repository.PasswordSecurityAuditRepository;
import com.tecngo.password_recovery.service.PasswordRecoveryService;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.verification.service.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordRecoveryServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final PasswordSecurityAuditRepository audits = mock(PasswordSecurityAuditRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final EmailSender emails = mock(EmailSender.class);
    private final PasswordRecoveryService service =
            new PasswordRecoveryService(users, tokens, audits, encoder, emails,
                    mock(com.tecngo.auth.session.AuthSessionService.class));

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "expirationMinutes", 30L);
        ReflectionTestUtils.setField(service, "resetUrl", "https://tecn-go.com/reset-password");
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() {
        when(users.findByEmailIgnoreCase("missing@tecngo.test")).thenReturn(Optional.empty());

        var response = service.forgotPassword("missing@tecngo.test");

        assertThat(response.message()).isEqualTo(PasswordRecoveryService.GENERIC_MESSAGE);
        verifyNoInteractions(emails);
    }

    @Test
    void validTokenChangesPasswordAndMarksTokensUsed() throws Exception {
        String rawToken = "valid-token";
        User user = User.builder().id(UUID.randomUUID()).password("old-hash").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user).token(hash(rawToken)).expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokens.findByToken(hash(rawToken))).thenReturn(Optional.of(token));
        when(encoder.encode("NewPassword123!")).thenReturn("new-hash");

        var response = service.resetPassword(
                new ResetPasswordRequest(rawToken, "NewPassword123!", "NewPassword123!"));

        assertThat(response.message()).isEqualTo("Contraseña actualizada correctamente.");
        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(tokens).invalidateActiveByUserId(eq(user.getId()), any(Instant.class));
        verify(audits).save(any());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        String rawToken = "expired-token";
        PasswordResetToken token = PasswordResetToken.builder()
                .user(User.builder().id(UUID.randomUUID()).build())
                .token(hash(rawToken)).expiresAt(Instant.now().minusSeconds(1)).build();
        when(tokens.findByToken(hash(rawToken))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(
                new ResetPasswordRequest(rawToken, "NewPassword123!", "NewPassword123!")))
                .isInstanceOf(ConflictException.class);
    }

    private String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
