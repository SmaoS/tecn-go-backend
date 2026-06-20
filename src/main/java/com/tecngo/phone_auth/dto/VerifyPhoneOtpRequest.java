package com.tecngo.phone_auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPhoneOtpRequest(@NotBlank String phone, @NotBlank String code) {
}
