package com.tecngo.verification;

import com.tecngo.shared.exception.ConflictException;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.verification.entity.VerificationToken;
import com.tecngo.verification.repository.VerificationTokenRepository;
import com.tecngo.verification.service.EmailSender;
import com.tecngo.verification.service.EmailVerificationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {
    @Test
    void expiredTokenCannotBeUsed() {
        VerificationTokenRepository tokens = mock(VerificationTokenRepository.class);
        UserRepository users = mock(UserRepository.class);
        EmailVerificationService service = new EmailVerificationService(tokens, users, mock(EmailSender.class));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(VerificationToken.builder()
                .user(User.builder().email("expired@tecngo.test").build())
                .expiresAt(Instant.now().minusSeconds(1))
                .build()));

        assertThatThrownBy(() -> service.verify("expired-token"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Verification token expired");
    }
}
