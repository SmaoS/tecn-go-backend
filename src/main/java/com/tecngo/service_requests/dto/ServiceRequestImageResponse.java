package com.tecngo.service_requests.dto;

import com.tecngo.content_moderation.entity.ModerationStatus;
import java.time.Instant;
import java.util.UUID;

public record ServiceRequestImageResponse(
        UUID id, UUID serviceRequestId, String imageUrl, String publicId, UUID contentAssetId,
        ModerationStatus moderationStatus, Instant createdAt
) {}
