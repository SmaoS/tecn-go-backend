package com.tecngo.reports.dto;
import com.tecngo.reports.entity.ReportStatus;
import jakarta.validation.constraints.*;
public record UpdateReportRequest(@NotNull ReportStatus status, @Size(max=2000) String comment) {}
