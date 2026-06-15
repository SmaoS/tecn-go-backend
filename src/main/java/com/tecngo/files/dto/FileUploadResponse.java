package com.tecngo.files.dto;

import com.tecngo.content_moderation.entity.ModerationStatus;
import java.util.UUID;

public record FileUploadResponse(
        String fileName, String contentType, long size, String url,
        String secureUrl, String publicId, UUID contentAssetId,
        ModerationStatus moderationStatus
) {}
