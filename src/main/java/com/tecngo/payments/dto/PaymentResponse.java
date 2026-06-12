package com.tecngo.payments.dto;

import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.payments.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId, UUID serviceRequestId, UUID clientId, String clientName,
        UUID technicianId, String technicianName, BigDecimal amount, BigDecimal platformFee,
        BigDecimal technicianAmount, BigDecimal platformCommissionPercentage,
        PaymentStatus paymentStatus, PaymentMethod paymentMethod,
        boolean commissionWaived, String commissionWaivedReason, UUID referralRewardId,
        Instant createdAt
) {}
