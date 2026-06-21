package com.tecngo.outbox.service;

import com.tecngo.outbox.entity.OutboxEvent;
import com.tecngo.outbox.entity.OutboxStatus;
import com.tecngo.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {
    private final OutboxEventRepository events;

    @Value("${app.reliability.outbox.max-attempts:8}")
    private int maxAttempts;

    @Transactional
    public List<OutboxEvent> claim(int limit, int staleSeconds) {
        Instant now = Instant.now();
        List<OutboxEvent> claimed = events.findDispatchable(now,
                now.minus(staleSeconds, ChronoUnit.SECONDS),
                PageRequest.of(0, Math.max(1, Math.min(limit, 100))));
        claimed.forEach(event -> {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setLockedAt(now);
        });
        return List.copyOf(claimed);
    }

    @Transactional
    public void processed(UUID id) {
        events.findById(id).ifPresent(event -> {
            event.setStatus(OutboxStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            event.setLockedAt(null);
            event.setLastError(null);
        });
    }

    @Transactional
    public void failed(UUID id, Exception exception) {
        events.findById(id).ifPresent(event -> {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            event.setLockedAt(null);
            event.setLastError(cleanError(exception));
            if (attempts >= maxAttempts) {
                event.setStatus(OutboxStatus.DEAD);
            } else {
                event.setStatus(OutboxStatus.PENDING);
                long delaySeconds = Math.min(1800, 5L << Math.min(attempts - 1, 8));
                event.setAvailableAt(Instant.now().plusSeconds(delaySeconds));
            }
        });
    }

    private String cleanError(Exception exception) {
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
