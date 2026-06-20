package com.tecngo.auth.service;

import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.Role;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private final JwtService jwtService =
            new JwtService("plain-text-secret-with-dashes-123456789", 60_000);

    @Test
    void generatesAndValidatesTokenWithPlainTextSecret() {
        var user = com.tecngo.users.entity.User.builder()
                .email("client@tecngo.local")
                .password("not-used")
                .role(Role.CLIENT)
                .roles(new LinkedHashSet<>(java.util.List.of(Role.CLIENT, Role.TECHNICIAN)))
                .activeMode(ActiveMode.TECHNICIAN)
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getUsername());
        assertThat(jwtService.extractRole(token)).isEqualTo("CLIENT");
        assertThat(jwtService.extractRoles(token)).containsExactly("CLIENT", "TECHNICIAN");
        assertThat(jwtService.extractActiveMode(token)).isEqualTo("TECHNICIAN");
        assertThat(jwtService.isValid(token, user)).isTrue();
        assertThat(token).isNotBlank();
    }

    @Test
    void keepsReadingLegacySingleRoleTokens() {
        var legacyUser = org.springframework.security.core.userdetails.User
                .withUsername("legacy@tecngo.local")
                .password("not-used")
                .roles("CLIENT")
                .build();

        String token = jwtService.generateToken(legacyUser);

        assertThat(jwtService.extractRole(token)).isEqualTo("CLIENT");
        assertThat(jwtService.extractRoles(token)).containsExactly("CLIENT");
        assertThat(jwtService.extractActiveMode(token)).isNull();
    }
}
