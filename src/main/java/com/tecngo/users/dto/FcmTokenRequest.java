package com.tecngo.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmTokenRequest(@NotBlank @Size(max = 500) String token) {}
