package com.tecngo.wompi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.reliability.wompi-reconciliation", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WompiReconciliationJob {
    private final WompiReconciliationService reconciliation;

    @Value("${app.reliability.wompi-reconciliation.batch-size:25}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.reliability.wompi-reconciliation.polling-ms:900000}")
    public void reconcile() {
        reconciliation.reconcile(batchSize);
    }
}
