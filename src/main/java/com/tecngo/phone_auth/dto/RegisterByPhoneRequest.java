package com.tecngo.phone_auth.dto;

import com.tecngo.users.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegisterByPhoneRequest(
        @NotBlank String fullName,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "El celular debe tener exactamente 10 dígitos") String phone,
        @NotBlank String verificationToken,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String confirmPassword,
        @NotNull Role role,
        String referralCode,
        UUID countryId
) {
}
