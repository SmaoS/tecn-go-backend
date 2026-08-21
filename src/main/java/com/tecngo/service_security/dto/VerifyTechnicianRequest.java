package com.tecngo.service_security.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyTechnicianRequest(@NotBlank String code) {
}
