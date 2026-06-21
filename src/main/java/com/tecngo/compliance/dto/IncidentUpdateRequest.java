package com.tecngo.compliance.dto;

import com.tecngo.compliance.entity.IncidentSeverity;
import com.tecngo.compliance.entity.IncidentStatus;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record IncidentUpdateRequest(
        @NotNull IncidentStatus status,
        IncidentSeverity severity,
        UUID assignedToUserId,
        @Size(max = 4000) String resolutionSummary
) {}
