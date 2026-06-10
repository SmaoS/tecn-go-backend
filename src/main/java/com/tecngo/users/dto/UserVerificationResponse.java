package com.tecngo.users.dto;

import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

public record UserVerificationResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        VerificationStatus verificationStatus,
        String profilePhotoUrl,
        String documentPhotoUrl,
        String certificatePhotoUrl,
        String workExperienceDescription,
        Instant createdAt,
        Instant verifiedAt
) {}
