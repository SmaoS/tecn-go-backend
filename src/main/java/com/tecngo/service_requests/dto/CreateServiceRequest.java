package com.tecngo.service_requests.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import com.tecngo.payments.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateServiceRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 1000) String description,
        @NotBlank String address,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @DecimalMin("0.00") BigDecimal estimatedPrice,
        UUID cityId,
        PaymentMethod paymentMethod
) {}
