package com.tecngo.technician_wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TechnicianWalletResponse(
        UUID walletId,
        UUID technicianId,
        String technicianName,
        String technicianEmail,
        String technicianPhone,
        BigDecimal balance,
        String currency,
        boolean rechargeEnabled,
        boolean lowBalance,
        boolean blocked,
        BigDecimal lowBalanceMinimum,
        BigDecimal minRechargeAmount,
        BigDecimal maxRechargeAmount,
        BigDecimal totalApprovedRecharges,
        BigDecimal totalCommissionDebits,
        long completedServicesCount,
        Instant updatedAt
) {}
