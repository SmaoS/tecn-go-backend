package com.tecngo.users.controller;

import com.tecngo.users.dto.UserVerificationResponse;
import com.tecngo.users.dto.ProfileSelfieChangeRequestResponse;
import com.tecngo.users.dto.RejectProfileSelfieChangeRequest;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.ProfileSelfieChangeRequestService;
import com.tecngo.users.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/verifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
public class VerificationController {
    private final VerificationService service;
    private final ProfileSelfieChangeRequestService profileSelfieChanges;

    @GetMapping("/pending")
    public List<UserVerificationResponse> pending() {
        return service.pending();
    }

    @PutMapping("/{userId}/verify")
    public UserVerificationResponse verify(@PathVariable UUID userId,
                                           @AuthenticationPrincipal User reviewer) {
        return service.verify(userId, reviewer);
    }

    @PutMapping("/{userId}/reject")
    public UserVerificationResponse reject(@PathVariable UUID userId,
                                           @AuthenticationPrincipal User reviewer) {
        return service.reject(userId, reviewer);
    }

    @PutMapping("/{userId}/profile-photo/verify")
    public UserVerificationResponse verifyProfilePhoto(@PathVariable UUID userId,
                                                       @AuthenticationPrincipal User reviewer) {
        return service.verifyProfilePhoto(userId, reviewer);
    }

    @GetMapping("/profile-selfie-change-requests/pending")
    public List<ProfileSelfieChangeRequestResponse> pendingProfileSelfieChanges() {
        return profileSelfieChanges.pending();
    }

    @PutMapping("/profile-selfie-change-requests/{id}/approve")
    public ProfileSelfieChangeRequestResponse approveProfileSelfieChange(@PathVariable UUID id,
                                                                         @AuthenticationPrincipal User reviewer) {
        return profileSelfieChanges.approve(id, reviewer);
    }

    @PutMapping("/profile-selfie-change-requests/{id}/reject")
    public ProfileSelfieChangeRequestResponse rejectProfileSelfieChange(
            @PathVariable UUID id,
            @Valid @RequestBody RejectProfileSelfieChangeRequest request,
            @AuthenticationPrincipal User reviewer) {
        return profileSelfieChanges.reject(id, request.reason(), reviewer);
    }
}
