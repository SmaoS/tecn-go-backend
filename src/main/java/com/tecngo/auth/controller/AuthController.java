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

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final PhoneOtpService phoneOtps;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
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
    public AuthResponse loginByPhone(@Valid @RequestBody LoginByPhoneRequest request) {
        return authService.loginByPhone(request);
    }

    @PostMapping("/forgot-password")
    public PasswordMessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
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
}
