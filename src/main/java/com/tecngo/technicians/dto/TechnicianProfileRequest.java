package com.tecngo.technicians.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record TechnicianProfileRequest(
        @NotBlank String documentNumber,
        @NotBlank String phone,
        @NotEmpty Set<UUID> categoryIds,
        @NotBlank @Size(max = 1000) String description,
        String profilePhotoUrl,
        @NotBlank String documentPhotoUrl,
        String certificatePhotoUrl,
        @NotBlank @Size(max = 1000) String workExperienceDescription,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank @Size(max = 255) String homeAddress,
        @NotNull Double homeLatitude,
        @NotNull Double homeLongitude,
        @Size(max = 120) String homeCity,
        @Size(max = 120) String homeNeighborhood,
        UUID countryId,
        UUID departmentId,
        UUID cityId
) {}
