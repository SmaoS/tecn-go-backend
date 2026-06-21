package com.tecngo.compliance.dto;

import com.tecngo.compliance.entity.IncidentSeverity;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public record IncidentRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull IncidentSeverity severity,
        Instant detectedAt,
        UUID assignedToUserId
) {}
