package com.tecngo.technician_wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import jakarta.validation.constraints.Pattern;

public record RechargeRequest(
        @NotNull @Positive BigDecimal amount,
        @Pattern(regexp = "WEB|MOBILE") String platform
) {}
