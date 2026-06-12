package com.tecngo.app_versions.dto;
import com.tecngo.app_versions.entity.AppPlatform;
import java.time.Instant;
import java.util.UUID;
public record AppVersionResponse(UUID id, AppPlatform platform, String minimumSupportedVersion,
        String latestVersion, boolean forceUpdate, String updateUrl, String message,
        boolean active, Instant createdAt, Instant updatedAt) {}
