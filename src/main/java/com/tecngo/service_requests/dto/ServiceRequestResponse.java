package com.tecngo.service_requests.dto;

import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.geolocation.LocationPrecision;

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
        boolean certifiedTechnician,
        UUID categoryId, String categoryName, String description, String address,
        Double latitude, Double longitude, LocationPrecision locationPrecision,
        Double distanceKm, BigDecimal estimatedPrice,
        BigDecimal technicianPrice, BigDecimal finalPrice, PaymentMethod requestedPaymentMethod,
        RequestStatus status, Instant createdAt,
        long serviceImagesCount, String firstServiceImageUrl, List<ServiceRequestImageResponse> images,
        UUID cityId, String cityName, boolean myPendingQuote
) {}
