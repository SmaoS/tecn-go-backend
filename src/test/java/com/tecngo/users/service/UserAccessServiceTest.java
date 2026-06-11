package com.tecngo.users.service;

import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.dto.InactivateUserRequest;
import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.InactivationReason;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAccessServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserAccessService service = new UserAccessService(users);

    @Test
    void inactiveUserCannotExecuteCriticalActions() {
        User user = User.builder().accountStatus(AccountStatus.INACTIVE_REPORT).build();
        assertThatThrownBy(() -> service.requireActive(user)).isInstanceOf(ConflictException.class);
    }

    @Test
    void adminCanInactivateClientAndAuditReason() {
        UUID id = UUID.randomUUID();
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        User client = User.builder().id(id).role(Role.CLIENT).accountStatus(AccountStatus.ACTIVE).build();
        when(users.findById(id)).thenReturn(Optional.of(client));

        var response = service.inactivate(id,
                new InactivateUserRequest(InactivationReason.PAYMENT_ISSUE, "Comprobante rechazado"),
                admin);

        assertThat(response.accountStatus()).isEqualTo(AccountStatus.INACTIVE_PAYMENT);
        assertThat(client.getInactivatedBy()).isEqualTo(admin);
        assertThat(client.getInactivatedAt()).isNotNull();
    }

    @Test
    void verifierCannotInactivateUsers() {
        User verifier = User.builder().role(Role.VERIFIER).build();
        assertThatThrownBy(() -> service.inactivate(UUID.randomUUID(),
                new InactivateUserRequest(InactivationReason.REPORT, "Escalado"), verifier))
                .isInstanceOf(ForbiddenException.class);
    }
}
