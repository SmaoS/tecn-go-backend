package com.tecngo.users.dto;

import com.tecngo.users.entity.FaceDetectionStatus;
import jakarta.validation.constraints.NotBlank;

public record ProfileSelfieChangeRequestCreate(
        @NotBlank String profilePhotoUrl,
        FaceDetectionStatus faceDetectionStatus
) {}
