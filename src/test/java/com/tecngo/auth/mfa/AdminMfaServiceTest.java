package com.tecngo.auth.mfa;

import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.verification.service.EmailSender;
import com.tecngo.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminMfaServiceTest {
    @Test
    void challengeCodeCanOnlyBeConsumedOnce() {
        AdminMfaChallengeRepository repository = mock(AdminMfaChallengeRepository.class);
        EmailSender emails = mock(EmailSender.class);
        AdminMfaService service = new AdminMfaService(
                repository, new BCryptPasswordEncoder(), emails);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "expirationMinutes", 10L);
        User admin = User.builder()
                .id(UUID.randomUUID())
                .fullName("Admin")
                .email("admin@tecngo.com")
                .role(Role.ADMIN)
                .build();
        AtomicReference<AdminMfaChallenge> saved = new AtomicReference<>();
        AtomicReference<String> sentCode = new AtomicReference<>();
        when(repository.save(any())).thenAnswer(invocation -> {
            AdminMfaChallenge challenge = invocation.getArgument(0);
            saved.set(challenge);
            return challenge;
        });
        doAnswer(invocation -> {
            sentCode.set(invocation.getArgument(2));
            return null;
        }).when(emails).sendMfaCode(eq(admin.getEmail()), eq(admin.getFullName()),
                anyString(), eq(10L));

        AdminMfaService.Challenge challenge = service.challenge(admin, "127.0.0.1");
        when(repository.findByChallengeTokenHash(anyString()))
                .thenReturn(Optional.of(saved.get()));

        User verified = service.verify(challenge.token(), sentCode.get());

        assertThat(verified).isSameAs(admin);
        assertThat(saved.get().getConsumedAt()).isNotNull();
        assertThatThrownBy(() -> service.verify(challenge.token(), sentCode.get()))
                .isInstanceOf(ConflictException.class);
        verify(emails).sendMfaCode(eq(admin.getEmail()), eq(admin.getFullName()),
                matches("\\d{6}"), eq(10L));
    }
}
