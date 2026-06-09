package com.tecngo.ratings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TechnicianRatingSummary(
        UUID technicianId, String technicianName, BigDecimal averageScore, long ratingCount
) {}
