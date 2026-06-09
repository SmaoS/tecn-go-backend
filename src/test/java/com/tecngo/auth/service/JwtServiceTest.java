package com.tecngo.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private final JwtService jwtService =
            new JwtService("plain-text-secret-with-dashes-123456789", 60_000);

    @Test
    void generatesAndValidatesTokenWithPlainTextSecret() {
        var user = User.withUsername("client@tecngo.local")
                .password("not-used")
                .roles("CLIENT")
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getUsername());
        assertThat(jwtService.extractRole(token)).isEqualTo("CLIENT");
        assertThat(jwtService.isValid(token, user)).isTrue();
        assertThat(token).isNotBlank();
    }
}
