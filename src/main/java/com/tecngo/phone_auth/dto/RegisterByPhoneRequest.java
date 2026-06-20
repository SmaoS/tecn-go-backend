package com.tecngo.phone_auth.dto;

import com.tecngo.users.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterByPhoneRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        @NotBlank String verificationToken,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String confirmPassword,
        @NotNull Role role,
        String referralCode
) {
}
