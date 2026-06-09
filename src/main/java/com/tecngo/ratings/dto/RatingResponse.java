package com.tecngo.ratings.dto;

import java.time.Instant;
import java.util.UUID;

public record RatingResponse(
        UUID id, UUID serviceRequestId, UUID raterId, String raterName,
        UUID ratedUserId, String ratedUserName, int score, String comment, Instant createdAt
) {}
