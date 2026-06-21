package com.tecngo.compliance.dto;

import java.time.Instant;
import java.util.UUID;

public record RetentionPolicyResponse(
        UUID id, String dataCategory, int retentionDays, String legalBasis,
        boolean automaticDeletion, boolean active, Instant updatedAt
) {}
