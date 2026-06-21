package com.tecngo.wompi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WompiReconciliationJob {
    private final WompiReconciliationService reconciliation;

    @Value("${app.reliability.wompi-reconciliation.batch-size:25}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.reliability.wompi-reconciliation.polling-ms:60000}")
    public void reconcile() {
        reconciliation.reconcile(batchSize);
    }
}
