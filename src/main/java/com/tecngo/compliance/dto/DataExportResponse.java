package com.tecngo.compliance.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DataExportResponse(UUID requestId, Instant generatedAt, Map<String, Object> data) {}
