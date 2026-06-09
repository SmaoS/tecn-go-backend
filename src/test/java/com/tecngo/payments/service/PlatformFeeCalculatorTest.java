package com.tecngo.payments.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformFeeCalculatorTest {
    private final PlatformFeeCalculator calculator = new PlatformFeeCalculator(BigDecimal.TEN);

    @Test
    void calculatesTenPercentFeeAndTechnicianAmount() {
        BigDecimal amount = new BigDecimal("125000.00");

        assertThat(calculator.fee(amount)).isEqualByComparingTo("12500.00");
        assertThat(calculator.technicianAmount(amount)).isEqualByComparingTo("112500.00");
    }

    @Test
    void roundsMonetaryFeeToTwoDecimals() {
        assertThat(calculator.fee(new BigDecimal("100.05"))).isEqualByComparingTo("10.01");
    }
}
