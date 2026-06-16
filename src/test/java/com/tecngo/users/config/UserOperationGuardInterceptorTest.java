package com.tecngo.users.config;

import com.tecngo.users.entity.User;
import com.tecngo.users.entity.Role;
import com.tecngo.users.service.UserOperationGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserOperationGuardInterceptorTest {
    private final UserOperationGuard guard = mock(UserOperationGuard.class);
    private final UserOperationGuardInterceptor interceptor = new UserOperationGuardInterceptor(guard);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usesServletPathWithoutApiContextPath() {
        User user = User.builder().role(Role.CLIENT).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me/onboarding-status");
        request.setContextPath("/api");
        request.setServletPath("/v1/users/me/onboarding-status");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verify(guard).requireAllowed(user, "GET", "/v1/users/me/onboarding-status");
    }
}
