package com.tecngo.service_evidence.dto;

import com.tecngo.service_evidence.entity.EvidenceType;
import com.tecngo.users.entity.Role;
import java.time.Instant;
import java.util.UUID;

public record ServiceEvidenceResponse(UUID id, UUID serviceRequestId, UUID uploadedByUserId,
        String uploadedByName, Role uploadedByRole, EvidenceType evidenceType, String fileUrl,
        String description, Instant createdAt) {}
