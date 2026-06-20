package com.tecngo.users.entity;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserMultiRoleTest {

    @Test
    void exposesAllPersistedRolesAsSpringAuthorities() {
        User user = User.builder()
                .role(Role.CLIENT)
                .roles(new LinkedHashSet<>(List.of(Role.CLIENT, Role.TECHNICIAN)))
                .activeMode(ActiveMode.CLIENT)
                .build();

        assertThat(user.getEffectiveRoles()).containsExactlyInAnyOrder(Role.CLIENT, Role.TECHNICIAN);
        assertThat(user.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_CLIENT", "ROLE_TECHNICIAN");
        assertThat(user.hasRole(Role.TECHNICIAN)).isTrue();
    }

    @Test
    void fallsBackToLegacyRoleWhenRoleCollectionIsEmpty() {
        User user = User.builder().role(Role.VERIFIER).build();

        assertThat(user.getEffectiveRoles()).containsExactly(Role.VERIFIER);
        assertThat(user.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_VERIFIER");
    }

    @Test
    void addingTechnicianCapabilityKeepsExistingClientMode() {
        User user = User.builder()
                .role(Role.CLIENT)
                .activeMode(ActiveMode.CLIENT)
                .build();

        user.addRole(Role.TECHNICIAN);

        assertThat(user.getEffectiveRoles()).contains(Role.CLIENT, Role.TECHNICIAN);
        assertThat(user.getActiveMode()).isEqualTo(ActiveMode.CLIENT);
    }
}
