package com.tecngo.technician_location.dto;

import com.tecngo.geolocation.LocationPrecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NearbyTechnicianResponse(
        UUID technicianId,
        String technicianName,
        String profilePhotoUrl,
        BigDecimal averageRating,
        long completedServicesCount,
        double latitude,
        double longitude,
        LocationPrecision locationPrecision,
        double distanceKm,
        Instant updatedAt
) {
}
