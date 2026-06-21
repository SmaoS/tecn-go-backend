package com.tecngo.auth.session;

import com.tecngo.auth.security.JwtAuthenticationFilter;
import com.tecngo.users.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users/me/sessions")
@RequiredArgsConstructor
public class AuthSessionController {
    private final AuthSessionService sessions;

    @GetMapping
    public List<AuthSessionResponse> mine(@AuthenticationPrincipal User user,
                                          HttpServletRequest request) {
        return sessions.mine(user, currentSessionId(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        sessions.revoke(id, user, "USER_REVOKED_SESSION");
    }

    private UUID currentSessionId(HttpServletRequest request) {
        Object value = request.getAttribute(JwtAuthenticationFilter.SESSION_ID_ATTRIBUTE);
        return value instanceof UUID id ? id : null;
    }
}
