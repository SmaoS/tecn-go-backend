package com.tecngo.compliance.dto;

import jakarta.validation.constraints.*;

public record RetentionPolicyRequest(
        @Min(1) @Max(36500) int retentionDays,
        @NotBlank @Size(max = 500) String legalBasis,
        boolean automaticDeletion,
        boolean active
) {}
