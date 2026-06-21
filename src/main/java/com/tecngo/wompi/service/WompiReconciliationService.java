package com.tecngo.wompi.service;

import com.tecngo.technician_wallet.dto.WompiReconciliationResponse;
import com.tecngo.technician_wallet.entity.TechnicianRecharge;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.tecngo.observability.BusinessMetrics;
import io.sentry.Sentry;

@Service
@RequiredArgsConstructor
@Slf4j
public class WompiReconciliationService {
    private final TechnicianWalletService wallets;
    private final WompiPaymentService wompi;
    private final BusinessMetrics metrics;

    public WompiReconciliationResponse reconcile(int limit) {
        var candidates = wallets.reconciliationCandidates(limit);
        int reconciled = 0;
        int failed = 0;
        for (TechnicianRecharge recharge : candidates) {
            try {
                wallets.applyWompiTransaction(
                        wompi.transaction(recharge.getWompiTransactionId()));
                reconciled++;
                metrics.wompiReconciled();
            } catch (Exception exception) {
                failed++;
                metrics.wompiReconciliationFailed();
                Sentry.captureException(exception);
                int delaySeconds = Math.min(3600,
                        60 << Math.min(recharge.getReconciliationAttempts(), 5));
                wallets.recordReconciliationFailure(
                        recharge.getReference(), exception, delaySeconds);
                log.warn("Wompi reconciliation failed for {}: {}",
                        recharge.getReference(), exception.getMessage());
            }
        }
        return new WompiReconciliationResponse(candidates.size(), reconciled, failed);
    }
}
