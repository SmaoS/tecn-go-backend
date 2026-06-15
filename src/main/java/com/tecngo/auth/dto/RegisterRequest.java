package com.tecngo.auth.dto;

import com.tecngo.users.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String confirmPassword,
        @NotNull Role role,
        String referralCode
) {}
