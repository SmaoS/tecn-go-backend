package com.tecngo.technician_location.dto;

import com.tecngo.geolocation.LocationPrecision;

import java.time.Instant;
import java.util.UUID;

public record TechnicianLocationResponse(
        UUID technicianId, String technicianName, double latitude, double longitude,
        LocationPrecision locationPrecision,
        Double accuracy, Double speed, Double heading, boolean online, Instant updatedAt
) {}
