package com.tecngo.users.repository;

import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserMultiRolePersistenceTest {
    @Autowired
    private UserRepository users;

    @Test
    void persistsLegacyRoleAsInitialCapabilityAndMode() {
        String suffix = UUID.randomUUID().toString();
        User saved = users.saveAndFlush(User.builder()
                .fullName("Multi Role")
                .email("multi-" + suffix + "@tecngo.local")
                .password("encoded")
                .role(Role.CLIENT)
                .build());

        User reloaded = users.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getRole()).isEqualTo(Role.CLIENT);
        assertThat(reloaded.getEffectiveRoles()).containsExactly(Role.CLIENT);
        assertThat(reloaded.getActiveMode()).isEqualTo(ActiveMode.CLIENT);
    }

    @Test
    void persistsAdditionalTechnicianCapability() {
        String suffix = UUID.randomUUID().toString();
        User user = User.builder()
                .fullName("Client Technician")
                .email("dual-" + suffix + "@tecngo.local")
                .password("encoded")
                .role(Role.CLIENT)
                .build();
        user.addRole(Role.TECHNICIAN);

        User saved = users.saveAndFlush(user);
        User reloaded = users.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getEffectiveRoles()).containsExactlyInAnyOrder(Role.CLIENT, Role.TECHNICIAN);
        assertThat(reloaded.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_CLIENT", "ROLE_TECHNICIAN");
    }
}
