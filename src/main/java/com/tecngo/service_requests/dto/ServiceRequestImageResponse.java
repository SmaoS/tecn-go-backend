package com.tecngo.service_requests.dto;

import java.time.Instant;
import java.util.UUID;

public record ServiceRequestImageResponse(
        UUID id, UUID serviceRequestId, String imageUrl, String publicId, Instant createdAt
) {}
