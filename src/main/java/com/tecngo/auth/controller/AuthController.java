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

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public PasswordMessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordRecoveryService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    public PasswordMessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordRecoveryService.resetPassword(request);
    }
}
