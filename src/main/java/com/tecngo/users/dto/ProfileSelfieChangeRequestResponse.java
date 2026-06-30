package com.tecngo.users.dto;

import com.tecngo.users.entity.FaceDetectionStatus;
import com.tecngo.users.entity.ProfileSelfieChangeRequestStatus;
import com.tecngo.users.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record ProfileSelfieChangeRequestResponse(
        UUID id,
        UUID userId,
        String userName,
        String userEmail,
        Role userRole,
        String currentPhotoUrl,
        String requestedPhotoUrl,
        FaceDetectionStatus faceDetectionStatus,
        ProfileSelfieChangeRequestStatus status,
        Instant requestedAt,
        UUID reviewedByUserId,
        Instant reviewedAt,
        String rejectionReason
) {}
