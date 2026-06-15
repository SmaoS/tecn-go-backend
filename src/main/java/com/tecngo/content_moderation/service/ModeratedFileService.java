package com.tecngo.content_moderation.service;

import com.tecngo.content_moderation.entity.*;
import com.tecngo.content_moderation.repository.ContentAssetRepository;
import com.tecngo.files.service.FileStorage;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ModeratedFileService {
    private final FileStorage storage;
    private final ImageModerationService moderation;
    private final ContentAssetRepository assets;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    @Transactional
    public StoredAsset store(MultipartFile file, String folder, Set<String> allowedTypes,
                             ContentAssetKind kind, User uploader) {
        FileStorage.StoredFile stored = storage.store(file, false, folder, allowedTypes);
        ImageModerationService.ModerationResult result = stored.contentType().startsWith("image/")
                ? moderation.moderate(stored.publicId())
                : new ImageModerationService.ModerationResult(
                        ModerationStatus.APPROVED, "Non-image file; image moderation not applicable");
        ContentAsset asset = assets.save(ContentAsset.builder()
                .uploadedBy(uploader)
                .kind(kind)
                .fileUrl(stored.accessUrl())
                .secureUrl(stored.secureUrl())
                .publicId(stored.publicId())
                .contentType(stored.contentType())
                .moderationStatus(result.status())
                .moderationReason(result.reason())
                .moderatedAt(result.status() == ModerationStatus.APPROVED
                        || result.status() == ModerationStatus.REJECTED ? Instant.now() : null)
                .build());
        if (result.status() != ModerationStatus.APPROVED) notifyReviewers(asset);
        return new StoredAsset(stored, asset);
    }

    private void notifyReviewers(ContentAsset asset) {
        users.findByRoleInOrderByCreatedAtDesc(Set.of(Role.ADMIN, Role.VERIFIER)).forEach(user ->
                events.publishEvent(new UserNotificationEvent(
                        user.getId(),
                        "Contenido pendiente de moderación",
                        asset.getKind() + " requiere revisión",
                        NotificationType.CONTENT_MODERATION_ALERT,
                        Map.of("type", "CONTENT_MODERATION", "route", "AdminOperations"))));
    }

    public record StoredAsset(FileStorage.StoredFile stored, ContentAsset asset) {}
}
