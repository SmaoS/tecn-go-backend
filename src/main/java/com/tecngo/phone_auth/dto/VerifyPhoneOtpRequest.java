package com.tecngo.phone_auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record VerifyPhoneOtpRequest(
        @NotBlank @Pattern(regexp = "\\d{10}", message = "El celular debe tener exactamente 10 dígitos") String phone,
        @NotBlank String code,
        UUID countryId
) {
}
