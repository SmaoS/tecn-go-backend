package com.tecngo.content_moderation.service;

import com.tecngo.content_moderation.entity.ModerationStatus;

public interface ImageModerationService {
    ModerationResult moderate(String publicId);

    record ModerationResult(ModerationStatus status, String reason) {}
}
