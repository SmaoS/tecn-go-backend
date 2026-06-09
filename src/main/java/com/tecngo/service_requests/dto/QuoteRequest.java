package com.tecngo.service_requests.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record QuoteRequest(@NotNull @DecimalMin("0.01") BigDecimal technicianPrice) {}
