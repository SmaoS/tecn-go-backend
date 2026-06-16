package com.tecngo.content_moderation.service;

import com.tecngo.content_moderation.dto.ContentAssetResponse;
import com.tecngo.content_moderation.entity.*;
import com.tecngo.content_moderation.repository.*;
import com.tecngo.payment_proofs.repository.PaymentProofRepository;
import com.tecngo.service_evidence.repository.ServiceEvidenceRepository;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.repository.ServiceRequestImageRepository;
import com.tecngo.shared.exception.*;
import com.tecngo.users.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentModerationService {
    private final ContentAssetRepository assets;
    private final ContentReportRepository reports;
    private final ServiceRequestImageRepository requestImages;
    private final ServiceEvidenceRepository evidences;
    private final PaymentProofRepository proofs;

    @Transactional(readOnly = true)
    public List<ContentAssetResponse> reviewQueue(ModerationStatus status, User reviewer) {
        requireReviewer(reviewer);
        List<ContentAsset> items = status == null
                ? assets.findAllByOrderByCreatedAtDesc()
                : assets.findByModerationStatusOrderByCreatedAtAsc(status);
        return items.stream().map(this::map).toList();
    }

    @Transactional
    public ContentAssetResponse decide(UUID id, ModerationStatus status, String reason, User reviewer) {
        requireReviewer(reviewer);
        if (status != ModerationStatus.APPROVED && status != ModerationStatus.REJECTED) {
            throw new IllegalArgumentException("Manual moderation must approve or reject content");
        }
        ContentAsset asset = requireAsset(id);
        asset.setModerationStatus(status);
        asset.setModerationReason(clean(reason));
        asset.setModeratedAt(Instant.now());
        asset.setModeratedBy(reviewer);
        if (status == ModerationStatus.APPROVED) {
            reports.findByStatusOrderByCreatedAtAsc(ContentReportStatus.OPEN).stream()
                    .filter(report -> report.getAsset().getId().equals(id))
                    .forEach(report -> report.setStatus(ContentReportStatus.RESOLVED));
        }
        return map(asset);
    }

    @Transactional
    public ContentAssetResponse report(UUID id, String reason, User user) {
        ContentAsset asset = requireAsset(id);
        if (!canView(asset, user, false)) throw new ForbiddenException("Content is not visible to this user");
        if (!reports.existsByAssetIdAndReportedByIdAndStatus(id, user.getId(), ContentReportStatus.OPEN)) {
            reports.save(ContentReport.builder()
                    .asset(asset).reportedBy(user).reason(reason.trim()).build());
        }
        if (asset.getModerationStatus() == ModerationStatus.APPROVED) {
            asset.setModerationStatus(ModerationStatus.FLAGGED);
            asset.setModerationReason("Reported by a user: " + reason.trim());
            asset.setModeratedAt(null);
            asset.setModeratedBy(null);
        }
        return map(asset);
    }

    @Transactional(readOnly = true)
    public boolean canDownload(ContentAsset asset, User viewer) {
        return canView(asset, viewer, true);
    }

    private boolean canView(ContentAsset asset, User viewer, boolean enforceStatus) {
        if (viewer == null) return false;
        boolean reviewer = viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.VERIFIER;
        if (enforceStatus && asset.getModerationStatus() == ModerationStatus.REJECTED && !reviewer) return false;
        if (reviewer) return true;
        if (asset.getKind() == ContentAssetKind.PROFILE) {
            if (asset.getUploadedBy().getId().equals(viewer.getId())) return true;
            return asset.getModerationStatus() == ModerationStatus.APPROVED
                    || asset.getUploadedBy().isProfilePhotoFaceValidated()
                    && asset.getFileUrl().equals(asset.getUploadedBy().getProfilePhotoUrl());
        }
        if (enforceStatus && asset.getModerationStatus() != ModerationStatus.APPROVED) return false;
        if (asset.getUploadedBy().getId().equals(viewer.getId())) return true;
        if (asset.getKind() == ContentAssetKind.DOCUMENT || asset.getKind() == ContentAssetKind.CERTIFICATE) return false;

        var image = requestImages.findByContentAssetId(asset.getId()).orElse(null);
        if (image != null) {
            var request = image.getServiceRequest();
            return request.getClient().getId().equals(viewer.getId())
                    || request.getTechnician() != null && request.getTechnician().getId().equals(viewer.getId())
                    || viewer.getRole() == Role.TECHNICIAN && request.getStatus() == RequestStatus.QUOTE_PENDING;
        }
        var evidence = evidences.findByContentAssetId(asset.getId()).orElse(null);
        if (evidence != null) return participant(evidence.getServiceRequest().getClient(),
                evidence.getServiceRequest().getTechnician(), viewer);
        var proof = proofs.findByContentAssetId(asset.getId()).orElse(null);
        return proof != null && participant(proof.getServiceRequest().getClient(),
                proof.getServiceRequest().getTechnician(), viewer);
    }

    private boolean participant(User client, User technician, User viewer) {
        return client.getId().equals(viewer.getId())
                || technician != null && technician.getId().equals(viewer.getId());
    }

    private ContentAsset requireAsset(UUID id) {
        return assets.findById(id).orElseThrow(() -> new NotFoundException("Content asset not found"));
    }

    private void requireReviewer(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.VERIFIER) {
            throw new ForbiddenException("Admin or verifier role is required");
        }
    }

    private ContentAssetResponse map(ContentAsset item) {
        return new ContentAssetResponse(item.getId(), item.getUploadedBy().getId(),
                item.getUploadedBy().getFullName(), item.getKind(), item.getFileUrl(),
                item.getContentType(), item.getModerationStatus(), item.getModerationReason(),
                item.getModeratedAt(),
                reports.countByAssetIdAndStatus(item.getId(), ContentReportStatus.OPEN),
                item.getCreatedAt());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
