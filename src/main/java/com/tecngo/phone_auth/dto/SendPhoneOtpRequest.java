package com.tecngo.phone_auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SendPhoneOtpRequest(@NotBlank String phone) {
}
