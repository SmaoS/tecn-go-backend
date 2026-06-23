package com.tecngo.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UserProfileRequest(
        @NotBlank String fullName,
        @Pattern(regexp = "^$|\\d{10}", message = "El celular debe tener exactamente 10 dígitos") String phone,
        String profilePhotoUrl,
        String documentPhotoUrl,
        String certificatePhotoUrl,
        @Size(max = 1000) String workExperienceDescription,
        @Size(max = 255) String homeAddress,
        Double homeLatitude,
        Double homeLongitude,
        @Size(max = 120) String homeCity,
        @Size(max = 120) String homeNeighborhood,
        UUID countryId,
        UUID departmentId,
        UUID cityId
) {}
