package com.tecngo.technician_wallet.dto;

public record WompiReconciliationResponse(
        int candidates,
        int reconciled,
        int failed
) {
}
