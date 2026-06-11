package com.tecngo.technician_location.dto;

import java.time.Instant;
import java.util.UUID;

public record TechnicianLocationResponse(
        UUID technicianId, String technicianName, double latitude, double longitude,
        Double accuracy, Double speed, Double heading, boolean online, Instant updatedAt
) {}
