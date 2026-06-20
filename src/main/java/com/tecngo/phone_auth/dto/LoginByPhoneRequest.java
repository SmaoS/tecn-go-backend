package com.tecngo.phone_auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginByPhoneRequest(@NotBlank String phone, @NotBlank String password) {
}
