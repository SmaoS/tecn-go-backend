package com.tecngo.auth.service;

import com.tecngo.auth.dto.*;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.UnauthorizedException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.verification.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.notifications.entity.NotificationType;
import java.util.Map;
import com.tecngo.referrals.service.ReferralService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final ApplicationEventPublisher events;
    private final ReferralService referrals;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (request.role() == Role.ADMIN || request.role() == Role.VERIFIER) {
            throw new IllegalArgumentException("This role cannot be registered publicly");
        }
        if (users.existsByEmailIgnoreCase(request.email())) throw new ConflictException("Email is already registered");
        User user = users.save(User.builder()
                .fullName(request.fullName().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .verificationStatus(VerificationStatus.CREATED)
                .build());
        referrals.register(user, request.referralCode());
        try {
            emailVerificationService.send(user);
        } catch (RuntimeException exception) {
            log.error("Account {} was created, but the verification email could not be sent",
                    user.getEmail(), exception);
        }
        events.publishEvent(new UserNotificationEvent(user.getId(), "Documentos legales pendientes",
                "Lee y acepta los términos, políticas y recomendaciones para usar todas las funciones de TecnGo.",
                NotificationType.LEGAL_ACCEPTANCE_REQUIRED,
                Map.of("type", "LEGAL", "route", "Legal")));
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            log.warn("Login rejected for {}", email);
            throw new UnauthorizedException("Correo o contraseña incorrectos");
        }
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Correo o contraseña incorrectos"));
        log.info("Login successful for {}", email);
        return response(user);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getFullName(),
                user.getEmail(), user.getRole(), user.getVerificationStatus(),
                user.isEmailVerified(), user.isPhoneVerified(), user.isDocumentsVerified());
    }
}
