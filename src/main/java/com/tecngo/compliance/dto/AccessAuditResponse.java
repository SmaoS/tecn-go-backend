package com.tecngo.compliance.dto;

import com.tecngo.compliance.entity.AuditOutcome;

import java.time.Instant;
import java.util.UUID;

public record AccessAuditResponse(
        UUID id, UUID actorUserId, UUID subjectUserId, String resourceType, String resourceId,
        String action, AuditOutcome outcome, String correlationId, String details, Instant createdAt
) {}
