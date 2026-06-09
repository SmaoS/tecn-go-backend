package com.tecngo.ratings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateRatingRequest(
        @Min(1) @Max(5) int score,
        @Size(max = 1000) String comment
) {}
