package com.tecngo.auth.service;

import com.tecngo.auth.dto.*;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.verification.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        try {
            emailVerificationService.send(user);
        } catch (RuntimeException exception) {
            log.error("Account {} was created, but the verification email could not be sent",
                    user.getEmail(), exception);
        }
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        User user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        return response(user);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getFullName(),
                user.getEmail(), user.getRole(), user.getVerificationStatus(),
                user.isEmailVerified(), user.isPhoneVerified(), user.isDocumentsVerified());
    }
}
