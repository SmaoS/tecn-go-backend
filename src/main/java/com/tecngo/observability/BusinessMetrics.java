package com.tecngo.observability;

import com.tecngo.outbox.entity.OutboxStatus;
import com.tecngo.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {
    private final MeterRegistry registry;
    private final Counter serverErrors;
    private final Counter wompiReconciled;
    private final Counter wompiReconciliationFailures;

    public BusinessMetrics(MeterRegistry registry, OutboxEventRepository outbox) {
        this.registry = registry;
        this.serverErrors = Counter.builder("tecngo.server.errors")
                .description("Unexpected API errors").register(registry);
        this.wompiReconciled = Counter.builder("tecngo.wompi.reconciled")
                .description("Wompi reconciliation successes").register(registry);
        this.wompiReconciliationFailures = Counter.builder("tecngo.wompi.reconciliation.failures")
                .description("Wompi reconciliation failures").register(registry);
        Gauge.builder("tecngo.outbox.pending", outbox,
                        repository -> repository.countByStatus(OutboxStatus.PENDING))
                .description("Pending outbox events").register(registry);
        Gauge.builder("tecngo.outbox.dead", outbox,
                        repository -> repository.countByStatus(OutboxStatus.DEAD))
                .description("Dead outbox events").register(registry);
    }

    public void serverError() {
        serverErrors.increment();
    }

    public void outboxProcessed(String eventType) {
        Counter.builder("tecngo.outbox.processed")
                .tag("event_type", eventType)
                .register(registry).increment();
    }

    public void outboxFailed(String eventType) {
        Counter.builder("tecngo.outbox.failures")
                .tag("event_type", eventType)
                .register(registry).increment();
    }

    public void wompiReconciled() {
        wompiReconciled.increment();
    }

    public void wompiReconciliationFailed() {
        wompiReconciliationFailures.increment();
    }
}
