package com.tecngo.auth.controller;

import com.tecngo.auth.dto.*;
import com.tecngo.auth.service.AuthService;
import com.tecngo.password_recovery.dto.ForgotPasswordRequest;
import com.tecngo.password_recovery.dto.PasswordMessageResponse;
import com.tecngo.password_recovery.dto.ResetPasswordRequest;
import com.tecngo.password_recovery.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.tecngo.phone_auth.dto.*;
import com.tecngo.phone_auth.service.PhoneOtpService;
import jakarta.servlet.http.HttpServletRequest;
import com.tecngo.auth.mfa.VerifyAdminMfaRequest;
import com.tecngo.auth.session.AuthSessionService;
import com.tecngo.auth.security.JwtAuthenticationFilter;
import com.tecngo.auth.ratelimit.SecurityRateLimitService;
import com.tecngo.users.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.Duration;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final PhoneOtpService phoneOtps;
    private final AuthSessionService sessions;
    private final SecurityRateLimitService rateLimits;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 HttpServletRequest servletRequest) {
        String ip = clientIp(servletRequest);
        rateLimits.check("REGISTER_IP", ip, 10, Duration.ofHours(1));
        rateLimits.record("REGISTER_IP", ip);
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest servletRequest) {
        return authService.login(request, clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
    }

    @PostMapping("/phone/send-otp")
    public SendPhoneOtpResponse sendPhoneOtp(@Valid @RequestBody SendPhoneOtpRequest request,
                                             HttpServletRequest servletRequest) {
        return phoneOtps.send(request.phone(), clientIp(servletRequest));
    }

    @PostMapping("/phone/verify-otp")
    public VerifyPhoneOtpResponse verifyPhoneOtp(@Valid @RequestBody VerifyPhoneOtpRequest request) {
        return phoneOtps.verify(request.phone(), request.code());
    }

    @PostMapping("/register-by-phone")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerByPhone(@Valid @RequestBody RegisterByPhoneRequest request) {
        return authService.registerByPhone(request);
    }

    @PostMapping("/login-by-phone")
    public AuthResponse loginByPhone(@Valid @RequestBody LoginByPhoneRequest request,
                                     HttpServletRequest servletRequest) {
        return authService.loginByPhone(request, clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"));
    }

    @PostMapping("/mfa/verify")
    public AuthResponse verifyMfa(@Valid @RequestBody VerifyAdminMfaRequest request,
                                  HttpServletRequest servletRequest) {
        return authService.verifyAdminMfa(request, clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal User user, HttpServletRequest request) {
        sessions.revoke(sessionId(request), user, "USER_LOGOUT");
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal User user) {
        sessions.revokeAll(user.getId(), "USER_LOGOUT_ALL");
    }

    @PostMapping("/forgot-password")
    public PasswordMessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                   HttpServletRequest servletRequest) {
        String ip = clientIp(servletRequest);
        rateLimits.check("PASSWORD_RESET_EMAIL", request.email(), 5, Duration.ofMinutes(15));
        rateLimits.check("PASSWORD_RESET_IP", ip, 20, Duration.ofMinutes(15));
        rateLimits.record("PASSWORD_RESET_EMAIL", request.email());
        rateLimits.record("PASSWORD_RESET_IP", ip);
        return passwordRecoveryService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    public PasswordMessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordRecoveryService.resetPassword(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
    }

    private java.util.UUID sessionId(HttpServletRequest request) {
        Object value = request.getAttribute(JwtAuthenticationFilter.SESSION_ID_ATTRIBUTE);
        return value instanceof java.util.UUID id ? id : null;
    }
}
