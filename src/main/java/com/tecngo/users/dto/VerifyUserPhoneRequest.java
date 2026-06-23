package com.tecngo.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyUserPhoneRequest(
        @NotBlank @Pattern(regexp = "\\d{10}", message = "El celular debe tener exactamente 10 dígitos") String phone,
        @NotBlank String verificationToken
) {
}
