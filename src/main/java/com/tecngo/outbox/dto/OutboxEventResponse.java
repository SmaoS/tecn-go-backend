package com.tecngo.outbox.dto;

import com.tecngo.outbox.entity.OutboxStatus;

import java.time.Instant;
import java.util.UUID;

public record OutboxEventResponse(
        UUID id,
        String eventType,
        String aggregateType,
        String aggregateId,
        OutboxStatus status,
        int attempts,
        Instant availableAt,
        Instant createdAt,
        Instant processedAt,
        String lastError
) {
}
