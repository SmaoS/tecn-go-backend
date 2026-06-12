package com.tecngo.app_versions.dto;
import com.tecngo.app_versions.entity.AppPlatform;
public record AppVersionCheckResponse(AppPlatform platform, String currentVersion, String latestVersion,
        String minimumSupportedVersion, boolean updateRequired, boolean forceUpdate,
        String updateUrl, String message) {}
