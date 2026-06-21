package com.tecngo.auth.security;

import com.tecngo.auth.service.JwtService;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.auth.session.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String SESSION_ID_ATTRIBUTE = "tecngo.auth.session-id";

    private final JwtService jwtService;
    private final UserRepository users;
    private final AuthSessionService sessions;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(7);
                String subject = jwtService.extractUsername(token);
                UUID sessionId = jwtService.extractSessionId(token);
                findUser(subject)
                        .filter(user -> jwtService.isValid(token, user))
                        .filter(user -> sessions.isActive(sessionId, user.getId()))
                        .ifPresent(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    request.setAttribute(SESSION_ID_ATTRIBUTE, sessionId);
                    sessions.touch(sessionId);
                });
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private Optional<com.tecngo.users.entity.User> findUser(String subject) {
        try {
            return users.findById(UUID.fromString(subject));
        } catch (IllegalArgumentException ignored) {
            return users.findByEmailIgnoreCase(subject)
                    .or(() -> users.findByPhoneNormalized(subject));
        }
    }
}
