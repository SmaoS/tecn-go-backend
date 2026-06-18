package com.tecngo.users.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record TechnicianProfessionalProfileRequest(
        @NotEmpty Set<UUID> categoryIds,
        @NotBlank @Size(min = 30, max = 1000) String workExperienceDescription
) {}
