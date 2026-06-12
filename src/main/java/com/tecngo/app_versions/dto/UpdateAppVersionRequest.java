package com.tecngo.app_versions.dto;
import jakarta.validation.constraints.NotBlank;
public record UpdateAppVersionRequest(@NotBlank String minimumSupportedVersion,
        @NotBlank String latestVersion, boolean forceUpdate, String updateUrl,
        @NotBlank String message, boolean active) {}
