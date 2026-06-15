package com.tecngo.content_moderation.controller;

import com.tecngo.content_moderation.dto.*;
import com.tecngo.content_moderation.entity.ModerationStatus;
import com.tecngo.content_moderation.service.ContentModerationService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ContentModerationController {
    private final ContentModerationService service;

    @PostMapping("/v1/content/{id}/report")
    public ContentAssetResponse report(@PathVariable UUID id,
                                       @Valid @RequestBody ContentReportRequest request,
                                       @AuthenticationPrincipal User user) {
        return service.report(id, request.reason(), user);
    }

    @GetMapping("/v1/admin/content-moderation")
    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
    public List<ContentAssetResponse> queue(@RequestParam(required = false) ModerationStatus status,
                                            @AuthenticationPrincipal User user) {
        return service.reviewQueue(status, user);
    }

    @PutMapping("/v1/admin/content-moderation/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
    public ContentAssetResponse approve(@PathVariable UUID id,
                                        @RequestBody(required = false) ModerationDecisionRequest request,
                                        @AuthenticationPrincipal User user) {
        return service.decide(id, ModerationStatus.APPROVED,
                request == null ? null : request.reason(), user);
    }

    @PutMapping("/v1/admin/content-moderation/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
    public ContentAssetResponse reject(@PathVariable UUID id,
                                       @Valid @RequestBody ModerationDecisionRequest request,
                                       @AuthenticationPrincipal User user) {
        return service.decide(id, ModerationStatus.REJECTED, request.reason(), user);
    }
}
