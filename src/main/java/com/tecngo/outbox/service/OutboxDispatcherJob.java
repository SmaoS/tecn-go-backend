package com.tecngo.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcherJob {
    private final OutboxClaimService claims;
    private final OutboxEventProcessor processor;

    @Value("${app.reliability.outbox.batch-size:25}")
    private int batchSize;
    @Value("${app.reliability.outbox.stale-seconds:120}")
    private int staleSeconds;

    @Scheduled(fixedDelayString = "${app.reliability.outbox.polling-ms:2000}")
    public void dispatch() {
        claims.claim(batchSize, staleSeconds).forEach(event -> {
            try {
                processor.process(event);
                claims.processed(event.getId());
            } catch (Exception exception) {
                log.warn("Outbox event {} ({}) failed: {}",
                        event.getId(), event.getEventType(), exception.getMessage());
                claims.failed(event.getId(), exception);
            }
        });
    }
}
