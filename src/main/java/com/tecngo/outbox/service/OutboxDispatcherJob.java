package com.tecngo.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.tecngo.observability.BusinessMetrics;
import io.sentry.Sentry;

@Component
@ConditionalOnProperty(prefix = "app.reliability.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcherJob {
    private final OutboxClaimService claims;
    private final OutboxEventProcessor processor;
    private final BusinessMetrics metrics;

    @Value("${app.reliability.outbox.batch-size:25}")
    private int batchSize;
    @Value("${app.reliability.outbox.stale-seconds:120}")
    private int staleSeconds;

    @Scheduled(fixedDelayString = "${app.reliability.outbox.polling-ms:300000}")
    public void dispatch() {
        claims.claim(batchSize, staleSeconds).forEach(event -> {
            try {
                processor.process(event);
                claims.processed(event.getId());
                metrics.outboxProcessed(event.getEventType());
            } catch (Exception exception) {
                log.warn("Outbox event {} ({}) failed: {}",
                        event.getId(), event.getEventType(), exception.getMessage());
                claims.failed(event.getId(), exception);
                metrics.outboxFailed(event.getEventType());
                Sentry.captureException(exception);
            }
        });
    }
}
