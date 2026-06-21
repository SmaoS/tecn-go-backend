package com.tecngo.auth.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyAdminMfaRequest(
        @NotBlank String challengeToken,
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {
}
