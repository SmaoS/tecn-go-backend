package com.tecngo.service_requests.dto;

import com.tecngo.service_requests.entity.QuoteStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ServiceQuoteResponse(
        UUID id,
        UUID serviceRequestId,
        UUID technicianId,
        String technicianName,
        String technicianProfilePhotoUrl,
        BigDecimal technicianAverageRating,
        long technicianCompletedServicesCount,
        String technicianExperienceDescription,
        List<String> technicianCategories,
        boolean technicianDocumentsVerified,
        boolean certifiedTechnician,
        BigDecimal price,
        String description,
        QuoteStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        Instant respondedAt
) {}
