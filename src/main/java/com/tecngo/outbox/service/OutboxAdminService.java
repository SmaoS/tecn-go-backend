package com.tecngo.outbox.service;

import com.tecngo.outbox.dto.OutboxEventResponse;
import com.tecngo.outbox.dto.OutboxSummaryResponse;
import com.tecngo.outbox.entity.OutboxEvent;
import com.tecngo.outbox.entity.OutboxStatus;
import com.tecngo.outbox.repository.OutboxEventRepository;
import com.tecngo.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxAdminService {
    private final OutboxEventRepository events;

    @Transactional(readOnly = true)
    public OutboxSummaryResponse summary() {
        return new OutboxSummaryResponse(
                events.countByStatus(OutboxStatus.PENDING),
                events.countByStatus(OutboxStatus.PROCESSING),
                events.countByStatus(OutboxStatus.PROCESSED),
                events.countByStatus(OutboxStatus.DEAD));
    }

    @Transactional(readOnly = true)
    public List<OutboxEventResponse> dead(int limit) {
        return events.findByStatusOrderByCreatedAtDesc(OutboxStatus.DEAD,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream().map(this::map).toList();
    }

    @Transactional
    public OutboxEventResponse retry(UUID id) {
        OutboxEvent event = events.findById(id)
                .orElseThrow(() -> new NotFoundException("Outbox event not found"));
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setAvailableAt(Instant.now());
        event.setLockedAt(null);
        event.setProcessedAt(null);
        event.setLastError(null);
        return map(event);
    }

    private OutboxEventResponse map(OutboxEvent event) {
        return new OutboxEventResponse(event.getId(), event.getEventType(),
                event.getAggregateType(), event.getAggregateId(), event.getStatus(),
                event.getAttempts(), event.getAvailableAt(), event.getCreatedAt(),
                event.getProcessedAt(), event.getLastError());
    }
}
