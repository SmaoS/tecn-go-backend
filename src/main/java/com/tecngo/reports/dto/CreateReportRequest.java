package com.tecngo.reports.dto;
import com.tecngo.reports.entity.*;
import jakarta.validation.constraints.*;
public record CreateReportRequest(@NotNull ReportReason reason, @NotBlank @Size(max=2000) String description,
                                  ReportSeverity severity) {}
