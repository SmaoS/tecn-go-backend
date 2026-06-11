package com.tecngo.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVerifierRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @Size(min = 8) String password,
        String homeAddress,
        Double homeLatitude,
        Double homeLongitude,
        String homeCity,
        String homeNeighborhood
) {}
