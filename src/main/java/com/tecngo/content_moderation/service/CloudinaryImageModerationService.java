package com.tecngo.content_moderation.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tecngo.content_moderation.entity.ModerationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.image-moderation", name = "provider", havingValue = "cloudinary")
public class CloudinaryImageModerationService implements ImageModerationService {
    private final Cloudinary cloudinary;
    @Value("${app.image-moderation.cloudinary-mode:aws_rek}")
    private String mode;

    @Override
    public ModerationResult moderate(String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().explicit(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "type", "authenticated",
                    "moderation", mode));
            Object raw = result.get("moderation");
            if (!(raw instanceof List<?> values) || values.isEmpty()) {
                return flagged("Cloudinary returned no moderation decision");
            }
            Object latest = values.getLast();
            if (!(latest instanceof Map<?, ?> decision)) {
                return flagged("Cloudinary returned an invalid moderation decision");
            }
            String status = String.valueOf(decision.get("status")).toLowerCase();
            String reason = mode + ": " + status;
            return switch (status) {
                case "approved" -> new ModerationResult(ModerationStatus.APPROVED, reason);
                case "rejected" -> new ModerationResult(ModerationStatus.REJECTED, reason);
                case "pending" -> new ModerationResult(ModerationStatus.PENDING_REVIEW, reason);
                default -> flagged("Unknown Cloudinary moderation status: " + status);
            };
        } catch (Exception exception) {
            log.warn("Cloudinary moderation unavailable for {}: {}", publicId, exception.getMessage());
            return flagged("Automatic moderation unavailable; manual review is required");
        }
    }

    private ModerationResult flagged(String reason) {
        return new ModerationResult(ModerationStatus.FLAGGED, reason);
    }
}
