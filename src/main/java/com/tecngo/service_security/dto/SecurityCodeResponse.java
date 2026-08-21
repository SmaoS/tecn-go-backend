package com.tecngo.service_security.dto;

import com.tecngo.service_security.entity.ServiceSecurityCodeStatus;

import java.time.Instant;

public record SecurityCodeResponse(
        String code,
        Instant expiresAt,
        ServiceSecurityCodeStatus status,
        int verificationAttempts,
        int maxAttempts,
        int regeneratedCount,
        int maxRegenerations
) {}
