package com.tecngo.users.service;

import com.tecngo.content_moderation.service.ManagedContentPolicy;
import com.tecngo.password_recovery.repository.PasswordResetTokenRepository;
import com.tecngo.password_recovery.repository.PasswordSecurityAuditRepository;
import com.tecngo.shared.exception.UnauthorizedException;
import com.tecngo.users.dto.ChangePasswordRequest;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServicePasswordTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(
            users, mock(ManagedContentPolicy.class), encoder,
            mock(PasswordResetTokenRepository.class), mock(PasswordSecurityAuditRepository.class),
            mock(com.tecngo.catalogs.service.GeographicCatalogService.class),
            mock(com.tecngo.phone_auth.service.PhoneNormalizer.class),
            mock(com.tecngo.phone_auth.service.PhoneOtpService.class),
            mock(com.tecngo.auth.session.AuthSessionService.class));

    @Test
    void changePasswordValidatesCurrentPassword() {
        User user = User.builder().password("stored-hash").build();
        when(encoder.matches("incorrect", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(user,
                new ChangePasswordRequest("incorrect", "NewPassword123!", "NewPassword123!")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("La contraseña actual es incorrecta");
        verify(users, never()).save(any());
    }
}
