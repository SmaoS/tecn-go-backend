package com.tecngo.auth.dto;

import com.tecngo.users.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @Size(min = 8) String password,
        @NotNull Role role,
        String profilePhotoUrl,
        @NotBlank String documentPhotoUrl,
        @Size(max = 500) String certificatePhotoUrl,
        @Size(max = 1000) String workExperienceDescription
) {}
