package com.tecngo.content_moderation.repository;

import com.tecngo.content_moderation.entity.ContentReport;
import com.tecngo.content_moderation.entity.ContentReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {
    boolean existsByAssetIdAndReportedByIdAndStatus(UUID assetId, UUID userId, ContentReportStatus status);
    long countByAssetIdAndStatus(UUID assetId, ContentReportStatus status);
    List<ContentReport> findByStatusOrderByCreatedAtAsc(ContentReportStatus status);
}
