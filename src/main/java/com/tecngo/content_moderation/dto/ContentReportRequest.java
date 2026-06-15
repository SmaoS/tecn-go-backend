package com.tecngo.content_moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentReportRequest(@NotBlank @Size(max = 1000) String reason) {}
