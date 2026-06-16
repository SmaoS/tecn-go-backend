package com.tecngo.technician_wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RechargeRequest(
        @NotNull @Positive BigDecimal amount
) {}
