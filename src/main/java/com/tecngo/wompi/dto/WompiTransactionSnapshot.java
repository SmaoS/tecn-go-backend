package com.tecngo.wompi.dto;

import java.math.BigDecimal;

public record WompiTransactionSnapshot(
        String transactionId,
        String reference,
        String status,
        BigDecimal amount,
        String currency
) {
}
