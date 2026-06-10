package com.tecngo.service_requests.dto;

import com.tecngo.service_requests.entity.RequestStatus;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

public record ServiceRequestResponse(
        UUID id, UUID clientId, String clientName, UUID technicianId, String technicianName,
        String clientProfilePhotoUrl, BigDecimal clientAverageRating, long clientPaidServicesCount,
        String technicianProfilePhotoUrl, BigDecimal technicianAverageRating,
        long technicianCompletedServicesCount, String technicianExperienceDescription,
        List<String> technicianCategories,
        UUID categoryId, String categoryName, String description, String address,
        Double latitude, Double longitude, Double distanceKm, BigDecimal estimatedPrice,
        BigDecimal technicianPrice, BigDecimal finalPrice, RequestStatus status, Instant createdAt
) {}
