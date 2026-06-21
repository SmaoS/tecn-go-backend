package com.tecngo.compliance.dto;

import com.tecngo.compliance.entity.DataRequestStatus;
import com.tecngo.compliance.entity.DataRequestType;

import java.time.Instant;
import java.util.UUID;

public record DataRequestResponse(
        UUID id, UUID userId, String userName, DataRequestType type, DataRequestStatus status,
        String reason, Instant requestedAt, Instant completedAt, UUID reviewedByUserId
) {}
