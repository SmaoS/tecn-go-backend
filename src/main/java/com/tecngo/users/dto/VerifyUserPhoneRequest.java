package com.tecngo.users.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyUserPhoneRequest(
        @NotBlank String phone,
        @NotBlank String verificationToken
) {
}
