package com.tecngo.technician_wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RechargeResponse(
        UUID rechargeId,
        String paymentUrl,
        String reference,
        BigDecimal amount,
        String currency
) {}
