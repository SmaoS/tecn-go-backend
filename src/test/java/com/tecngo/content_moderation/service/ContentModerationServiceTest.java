package com.tecngo.content_moderation.service;

import com.tecngo.content_moderation.entity.*;
import com.tecngo.content_moderation.repository.ContentAssetRepository;
import com.tecngo.content_moderation.repository.ContentReportRepository;
import com.tecngo.payment_proofs.repository.PaymentProofRepository;
import com.tecngo.service_evidence.entity.ServiceEvidence;
import com.tecngo.service_evidence.repository.ServiceEvidenceRepository;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestImageRepository;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentModerationServiceTest {
    private final ContentAssetRepository assets = mock(ContentAssetRepository.class);
    private final ContentReportRepository reports = mock(ContentReportRepository.class);
    private final ServiceRequestImageRepository requestImages = mock(ServiceRequestImageRepository.class);
    private final ServiceEvidenceRepository evidences = mock(ServiceEvidenceRepository.class);
    private final PaymentProofRepository proofs = mock(PaymentProofRepository.class);
    private final ContentModerationService service =
            new ContentModerationService(assets, reports, requestImages, evidences, proofs);

    private User client;
    private User technician;
    private ContentAsset asset;

    @BeforeEach
    void setUp() {
        client = User.builder().id(UUID.randomUUID()).fullName("Cliente").role(Role.CLIENT).build();
        technician = User.builder().id(UUID.randomUUID()).fullName("Tecnico").role(Role.TECHNICIAN).build();
        asset = ContentAsset.builder()
                .id(UUID.randomUUID())
                .uploadedBy(client)
                .kind(ContentAssetKind.SERVICE_EVIDENCE)
                .fileUrl("/v1/files/private-file")
                .contentType("image/jpeg")
                .moderationStatus(ModerationStatus.APPROVED)
                .createdAt(Instant.now())
                .build();
        when(assets.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(requestImages.findByContentAssetId(asset.getId())).thenReturn(Optional.empty());
        when(proofs.findByContentAssetId(asset.getId())).thenReturn(Optional.empty());
    }

    @Test
    void approvedEvidenceIsVisibleToServiceParticipant() {
        ServiceRequest request = ServiceRequest.builder().client(client).technician(technician).build();
        when(evidences.findByContentAssetId(asset.getId())).thenReturn(Optional.of(
                ServiceEvidence.builder().contentAsset(asset).serviceRequest(request).build()));

        assertThat(service.canDownload(asset, technician)).isTrue();
    }

    @Test
    void rejectedImageIsHiddenFromParticipantButVisibleToReviewer() {
        asset.setModerationStatus(ModerationStatus.REJECTED);
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();

        assertThat(service.canDownload(asset, technician)).isFalse();
        assertThat(service.canDownload(asset, admin)).isTrue();
    }

    @Test
    void nonParticipantCannotViewServiceEvidence() {
        User stranger = User.builder().id(UUID.randomUUID()).role(Role.CLIENT).build();
        ServiceRequest request = ServiceRequest.builder().client(client).technician(technician).build();
        when(evidences.findByContentAssetId(asset.getId())).thenReturn(Optional.of(
                ServiceEvidence.builder().contentAsset(asset).serviceRequest(request).build()));

        assertThat(service.canDownload(asset, stranger)).isFalse();
    }

    @Test
    void reportingApprovedContentFlagsItAndCreatesAdminQueueItem() {
        ServiceRequest request = ServiceRequest.builder().client(client).technician(technician).build();
        when(evidences.findByContentAssetId(asset.getId())).thenReturn(Optional.of(
                ServiceEvidence.builder().contentAsset(asset).serviceRequest(request).build()));
        when(reports.existsByAssetIdAndReportedByIdAndStatus(
                asset.getId(), technician.getId(), ContentReportStatus.OPEN)).thenReturn(false);
        when(reports.countByAssetIdAndStatus(asset.getId(), ContentReportStatus.OPEN)).thenReturn(1L);

        var response = service.report(asset.getId(), "Contenido inapropiado", technician);

        assertThat(response.moderationStatus()).isEqualTo(ModerationStatus.FLAGGED);
        assertThat(asset.getModerationReason()).contains("Contenido inapropiado");
        verify(reports).save(any(ContentReport.class));
    }

    @Test
    void onlyAdminOrVerifierCanModerateManually() {
        assertThatThrownBy(() -> service.decide(asset.getId(), ModerationStatus.REJECTED,
                "Contenido sexual", client)).isInstanceOf(ForbiddenException.class);

        User verifier = User.builder().id(UUID.randomUUID()).role(Role.VERIFIER).build();
        var response = service.decide(asset.getId(), ModerationStatus.REJECTED,
                "Contenido sexual", verifier);

        assertThat(response.moderationStatus()).isEqualTo(ModerationStatus.REJECTED);
        assertThat(asset.getModeratedBy()).isEqualTo(verifier);
    }
}
