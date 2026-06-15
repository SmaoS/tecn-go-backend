package com.tecngo.content_moderation.service;

import com.tecngo.content_moderation.entity.ContentAssetKind;
import com.tecngo.content_moderation.entity.ModerationStatus;
import com.tecngo.content_moderation.repository.ContentAssetRepository;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ManagedContentPolicy {
    private final ContentAssetRepository assets;

    @Value("${app.image-moderation.require-managed-uploads:true}")
    private boolean requireManagedUploads;

    public String validateChange(String currentUrl, String requestedUrl, User owner,
                                 Set<ContentAssetKind> allowedKinds) {
        String current = clean(currentUrl);
        String requested = clean(requestedUrl);
        if (Objects.equals(current, requested) || requested == null || !requireManagedUploads) {
            return requested;
        }
        var asset = assets.findByFileUrl(requested)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Files must be uploaded through the TecnGo upload endpoint"));
        if (!asset.getUploadedBy().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("The uploaded file does not belong to this user");
        }
        if (!allowedKinds.contains(asset.getKind())) {
            throw new IllegalArgumentException("The uploaded file has an invalid purpose");
        }
        if (asset.getModerationStatus() == ModerationStatus.REJECTED) {
            throw new IllegalArgumentException("The uploaded file was rejected by moderation");
        }
        return requested;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
