package com.tecngo.content_moderation.dto;

import com.tecngo.content_moderation.entity.ContentAssetKind;
import com.tecngo.content_moderation.entity.ModerationStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentAssetResponse(
        UUID id, UUID uploadedByUserId, String uploadedByName, ContentAssetKind kind,
        String fileUrl, String contentType, ModerationStatus moderationStatus,
        String moderationReason, Instant moderatedAt, long openReports, Instant createdAt
) {}
