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
import com.tecngo.referrals.service.ReferralService;
import com.tecngo.phone_auth.dto.LoginByPhoneRequest;
import com.tecngo.phone_auth.dto.RegisterByPhoneRequest;
import com.tecngo.phone_auth.service.PhoneNormalizer;
import com.tecngo.phone_auth.service.PhoneOtpService;
import com.tecngo.auth.mfa.AdminMfaService;
import com.tecngo.auth.ratelimit.SecurityRateLimitService;
import com.tecngo.auth.session.AuthSessionService;
import com.tecngo.auth.mfa.VerifyAdminMfaRequest;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;
    private final ReferralService referrals;
    private final PhoneOtpService phoneOtps;
    private final PhoneNormalizer phones;
    private final AdminMfaService adminMfa;
    private final SecurityRateLimitService rateLimits;
    private final AuthSessionService sessions;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (!request.email().trim().equalsIgnoreCase(request.confirmEmail().trim())) {
            throw new IllegalArgumentException("Los correos no coinciden");
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
        return response(user, null, null, false);
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, null, null);
    }

    public AuthResponse login(LoginRequest request, String clientIp, String userAgent) {
        String email = request.email().trim().toLowerCase();
        rateLimits.check("LOGIN_EMAIL", email, 10, Duration.ofMinutes(15));
        rateLimits.check("LOGIN_IP", clientIp, 30, Duration.ofMinutes(15));
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            rateLimits.record("LOGIN_EMAIL", email);
            rateLimits.record("LOGIN_IP", clientIp);
            log.warn("Login rejected for {}", email);
            throw new UnauthorizedException("Correo o contraseña incorrectos");
        }
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Correo o contraseña incorrectos"));
        log.info("Login successful for {}", email);
        return loginResponse(user, clientIp, userAgent);
    }

    @Transactional
    public AuthResponse registerByPhone(RegisterByPhoneRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (request.role() == Role.ADMIN || request.role() == Role.VERIFIER) {
            throw new IllegalArgumentException("This role cannot be registered publicly");
        }
        String localPhone = phones.local(request.phone());
        String normalizedPhone = phones.international(request.phone(), request.countryId());
        if (users.existsByPhoneNormalized(normalizedPhone)) {
            throw new ConflictException("Phone is already registered");
        }
        phoneOtps.consume(localPhone, request.countryId(), request.verificationToken());
        User user = users.save(User.builder()
                .fullName(request.fullName().trim())
                .phone(localPhone)
                .phoneNormalized(normalizedPhone)
                .phoneVerified(true)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .verificationStatus(VerificationStatus.CREATED)
                .build());
        referrals.register(user, request.referralCode());
        return response(user, null, null, false);
    }

    public AuthResponse loginByPhone(LoginByPhoneRequest request) {
        return loginByPhone(request, null, null);
    }

    public AuthResponse loginByPhone(LoginByPhoneRequest request, String clientIp, String userAgent) {
        String phone = phones.international(request.phone(), request.countryId());
        rateLimits.check("LOGIN_PHONE", phone, 10, Duration.ofMinutes(15));
        rateLimits.check("LOGIN_IP", clientIp, 30, Duration.ofMinutes(15));
        User user = users.findByPhoneNormalized(phone).orElse(null);
        if (user == null) {
            rateLimits.record("LOGIN_PHONE", phone);
            rateLimits.record("LOGIN_IP", clientIp);
            throw new UnauthorizedException("Celular o contraseña incorrectos");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            rateLimits.record("LOGIN_PHONE", phone);
            rateLimits.record("LOGIN_IP", clientIp);
            throw new UnauthorizedException("Celular o contraseña incorrectos");
        }
        return loginResponse(user, clientIp, userAgent);
    }

    public AuthResponse response(User user) {
        return response(user, null, null, false);
    }

    public AuthResponse verifyAdminMfa(VerifyAdminMfaRequest request, String clientIp, String userAgent) {
        User user = adminMfa.verify(request.challengeToken(), request.code());
        return response(user, clientIp, userAgent, true);
    }

    private AuthResponse loginResponse(User user, String clientIp, String userAgent) {
        if (adminMfa.required(user)) {
            AdminMfaService.Challenge challenge = adminMfa.challenge(user, clientIp);
            return new AuthResponse(null, user.getId(), user.getFullName(),
                    user.getEmail(), user.getRole(), user.getEffectiveRoles(), user.getActiveMode(),
                    user.getVerificationStatus(), user.isEmailVerified(), user.isPhoneVerified(),
                    user.isDocumentsVerified(), user.isOnboardingCompleted(),
                    true, challenge.token(), challenge.expiresAt());
        }
        return response(user, clientIp, userAgent, false);
    }

    private AuthResponse response(User user, String clientIp, String userAgent, boolean mfaVerified) {
        return new AuthResponse(sessions.issue(user, clientIp, userAgent, mfaVerified),
                user.getId(), user.getFullName(),
                user.getEmail(), user.getRole(), user.getEffectiveRoles(), user.getActiveMode(),
                user.getVerificationStatus(),
                user.isEmailVerified(), user.isPhoneVerified(), user.isDocumentsVerified(),
                user.isOnboardingCompleted(), false, null, null);
    }
}
