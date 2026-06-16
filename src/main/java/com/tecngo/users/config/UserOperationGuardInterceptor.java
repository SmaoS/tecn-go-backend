package com.tecngo.users.config;

import com.tecngo.users.entity.User;
import com.tecngo.users.service.UserOperationGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserOperationGuardInterceptor implements HandlerInterceptor {
    private final UserOperationGuard guard;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            guard.requireAllowed(user, request.getMethod(), request.getRequestURI());
        }
        return true;
    }
}
