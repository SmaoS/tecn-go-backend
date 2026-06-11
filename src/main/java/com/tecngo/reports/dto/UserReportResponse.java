package com.tecngo.reports.dto;
import com.tecngo.reports.entity.*;
import com.tecngo.users.entity.Role;
import java.time.Instant;
import java.util.UUID;
public record UserReportResponse(UUID id, UUID serviceRequestId, UUID reporterUserId, String reporterName,
        UUID reportedUserId, String reportedName, Role reporterRole, Role reportedRole,
        ReportReason reason, String description, ReportStatus status, ReportSeverity severity,
        Instant createdAt, UUID reviewedByUserId, Instant reviewedAt, String resolutionComment) {}
