package com.tecngo.users.service;

import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.content_moderation.service.ManagedContentPolicy;
import com.tecngo.password_recovery.repository.PasswordResetTokenRepository;
import com.tecngo.password_recovery.repository.PasswordSecurityAuditRepository;
import com.tecngo.phone_auth.service.PhoneNormalizer;
import com.tecngo.phone_auth.service.PhoneOtpService;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServicePhoneVerificationTest {
    private UserRepository users;
    private PhoneOtpService phoneOtps;
    private UserService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        phoneOtps = mock(PhoneOtpService.class);
        service = new UserService(
                users,
                mock(ManagedContentPolicy.class),
                mock(PasswordEncoder.class),
                mock(PasswordResetTokenRepository.class),
                mock(PasswordSecurityAuditRepository.class),
                mock(GeographicCatalogService.class),
                mock(PhoneNormalizer.class),
                phoneOtps,
                mock(com.tecngo.auth.session.AuthSessionService.class));
    }

    @Test
    void consumesOtpAndAssignsVerifiedPhoneToAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .fullName("Técnico")
                .averageRating(new BigDecimal("5.00"))
                .build();
        when(users.findProfileById(userId)).thenReturn(Optional.of(user));
        when(phoneOtps.consume("3001234567", null, "verified-token"))
                .thenReturn(new PhoneOtpService.VerifiedPhone("3001234567", "+573001234567"));
        when(users.findByPhoneNormalized("+573001234567")).thenReturn(Optional.empty());
        when(users.save(user)).thenReturn(user);

        var response = service.verifyPhone(user, "3001234567", "verified-token");

        assertThat(response.phone()).isEqualTo("3001234567");
        assertThat(response.phoneVerified()).isTrue();
        verify(users).save(user);
    }

    @Test
    void rejectsPhoneAlreadyOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).fullName("Técnico")
                .averageRating(new BigDecimal("5.00")).build();
        User owner = User.builder().id(UUID.randomUUID()).build();
        when(users.findProfileById(userId)).thenReturn(Optional.of(user));
        when(phoneOtps.consume("3001234567", null, "verified-token"))
                .thenReturn(new PhoneOtpService.VerifiedPhone("3001234567", "+573001234567"));
        when(users.findByPhoneNormalized("+573001234567")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.verifyPhone(user, "3001234567", "verified-token"))
                .isInstanceOf(ConflictException.class);
        verify(users, never()).save(user);
    }
}
