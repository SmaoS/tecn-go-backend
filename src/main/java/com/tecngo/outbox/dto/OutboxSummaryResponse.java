package com.tecngo.outbox.dto;

public record OutboxSummaryResponse(
        long pending,
        long processing,
        long processed,
        long dead
) {
}
