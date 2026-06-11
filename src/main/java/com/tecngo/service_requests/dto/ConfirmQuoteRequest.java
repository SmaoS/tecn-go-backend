package com.tecngo.service_requests.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmQuoteRequest(@NotNull UUID quoteId) {}
