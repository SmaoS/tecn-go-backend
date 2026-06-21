package com.tecngo.compliance.dto;

import com.tecngo.compliance.entity.IncidentSeverity;
import com.tecngo.compliance.entity.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id, String title, String description, IncidentSeverity severity, IncidentStatus status,
        Instant detectedAt, Instant containedAt, Instant resolvedAt, Instant createdAt,
        UUID reportedByUserId, UUID assignedToUserId, String resolutionSummary
) {}
