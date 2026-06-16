package com.tecngo.technician_wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdminWalletAdjustmentRequest(
        @NotNull BigDecimal amount,
        @NotBlank String comment
) {}
