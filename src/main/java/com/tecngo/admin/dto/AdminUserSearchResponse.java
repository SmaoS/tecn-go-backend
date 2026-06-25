package com.tecngo.admin.dto;

import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.OnboardingStep;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.VerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AdminUserSearchResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String profilePhotoUrl,
        Role primaryRole,
        Set<Role> roles,
        AccountStatus accountStatus,
        VerificationStatus verificationStatus,
        boolean onboardingCompleted,
        OnboardingStep onboardingStep,
        String onboardingStatus,
        List<String> onboardingComments,
        Instant createdAt
) {}
