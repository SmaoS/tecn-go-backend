package com.tecngo.users.dto;

import jakarta.validation.constraints.NotBlank;
import com.tecngo.users.entity.FaceDetectionStatus;

public record ProfileSelfieRequest(
        @NotBlank String profilePhotoUrl,
        FaceDetectionStatus faceDetectionStatus
) {}
