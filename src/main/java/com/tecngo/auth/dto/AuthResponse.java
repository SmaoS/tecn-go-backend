package com.tecngo.auth.dto;

import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.VerificationStatus;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String fullName,
        String email,
        Role role,
        VerificationStatus verificationStatus
) {}
