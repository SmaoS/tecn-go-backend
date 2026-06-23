package com.tecngo.users.dto;

import com.tecngo.users.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OnboardingMainDataRequest(
        @NotBlank String fullName,
        @Pattern(regexp = "^$|\\d{10}", message = "El celular debe tener exactamente 10 dígitos") String phone,
        @NotNull UUID countryId,
        @NotNull UUID departmentId,
        @NotNull UUID cityId,
        @NotBlank @Size(max = 255) String address,
        @Size(max = 120) String neighborhood,
        @NotNull DocumentType documentType,
        @NotBlank @Size(max = 50) String documentNumber
) {}
