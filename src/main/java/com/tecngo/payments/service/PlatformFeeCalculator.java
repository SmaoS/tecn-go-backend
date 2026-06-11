package com.tecngo.payments.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PlatformFeeCalculator {
    private final BigDecimal percentage;

    public PlatformFeeCalculator(@Value("${app.payments.platform-fee-percentage:10}") BigDecimal percentage) {
        if (percentage.signum() < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Platform fee percentage must be between 0 and 100");
        }
        this.percentage = percentage;
    }

    public BigDecimal fee(BigDecimal amount) {
        return fee(amount, percentage);
    }

    public BigDecimal fee(BigDecimal amount, BigDecimal appliedPercentage) {
        return amount.multiply(appliedPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal technicianAmount(BigDecimal amount) {
        return technicianAmount(amount, percentage);
    }

    public BigDecimal technicianAmount(BigDecimal amount, BigDecimal appliedPercentage) {
        return amount.subtract(fee(amount, appliedPercentage)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal percentage() {
        return percentage;
    }
}
