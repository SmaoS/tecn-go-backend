package com.tecngo.content_moderation.service;

import com.tecngo.content_moderation.entity.ModerationStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.image-moderation", name = "provider",
        havingValue = "manual", matchIfMissing = true)
public class ManualImageModerationService implements ImageModerationService {
    @Override
    public ModerationResult moderate(String publicId) {
        return new ModerationResult(ModerationStatus.FLAGGED,
                "Automatic moderation is not configured; manual review is required");
    }
}
