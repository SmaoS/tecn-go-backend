package com.tecngo.service_requests.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record QuoteRequest(
        @NotNull @DecimalMin("0.01") BigDecimal technicianPrice,
        @Size(max = 1000) String description
) {}
