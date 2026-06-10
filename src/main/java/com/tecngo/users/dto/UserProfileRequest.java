package com.tecngo.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(
        @NotBlank String fullName,
        String profilePhotoUrl,
        String documentPhotoUrl,
        String certificatePhotoUrl,
        @Size(max = 1000) String workExperienceDescription
) {}
