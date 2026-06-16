package com.tecngo.technician_wallet.dto;

import com.tecngo.technician_wallet.entity.WalletTransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TechnicianWalletTransactionResponse(
        UUID id,
        WalletTransactionType type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String reference,
        String description,
        Instant createdAt
) {}
