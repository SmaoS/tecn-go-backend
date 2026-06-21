package com.tecngo.auth.ratelimit;

import com.tecngo.shared.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecurityRateLimitServiceTest {
    private final SecurityRateLimitRepository repository = mock(SecurityRateLimitRepository.class);
    private final SecurityRateLimitService service = new SecurityRateLimitService(repository);

    @Test
    void rejectsAKeyThatReachedItsLimit() {
        when(repository.countByActionAndKeyHashAndCreatedAtAfter(
                eq("LOGIN_EMAIL"), anyString(), any())).thenReturn(10L);

        assertThatThrownBy(() -> service.check(
                "LOGIN_EMAIL", "user@tecngo.com", 10, Duration.ofMinutes(15)))
                .isInstanceOf(TooManyRequestsException.class)
                .extracting("code")
                .isEqualTo("AUTH_RATE_LIMITED");
    }
}
