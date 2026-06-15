package com.tecngo.service_evidence.dto;

import com.tecngo.service_evidence.entity.EvidenceType;
import com.tecngo.users.entity.Role;
import com.tecngo.content_moderation.entity.ModerationStatus;
import java.time.Instant;
import java.util.UUID;

public record ServiceEvidenceResponse(UUID id, UUID serviceRequestId, UUID uploadedByUserId,
        String uploadedByName, Role uploadedByRole, EvidenceType evidenceType, String fileUrl,
        UUID contentAssetId, ModerationStatus moderationStatus, String description, Instant createdAt) {}
