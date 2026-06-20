package com.tecngo.auth.dto;

import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.VerificationStatus;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String fullName,
        String email,
        Role role,
        Set<Role> roles,
        ActiveMode activeMode,
        VerificationStatus verificationStatus,
        boolean emailVerified,
        boolean phoneVerified,
        boolean documentsVerified,
        boolean onboardingCompleted
) {}
