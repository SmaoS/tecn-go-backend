package com.tecngo.payments.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinancialSummaryResponse(
        BigDecimal totalAmount, BigDecimal totalPlatformFee, BigDecimal totalTechnicianAmount,
        long paymentCount, List<PaymentResponse> payments
) {}
