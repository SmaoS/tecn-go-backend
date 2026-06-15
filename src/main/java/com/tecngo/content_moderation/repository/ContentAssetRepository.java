package com.tecngo.content_moderation.repository;

import com.tecngo.content_moderation.entity.ContentAsset;
import com.tecngo.content_moderation.entity.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentAssetRepository extends JpaRepository<ContentAsset, UUID> {
    Optional<ContentAsset> findByFileUrl(String fileUrl);
    List<ContentAsset> findByModerationStatusOrderByCreatedAtAsc(ModerationStatus status);
    List<ContentAsset> findAllByOrderByCreatedAtDesc();
}
