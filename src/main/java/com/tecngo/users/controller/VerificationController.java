package com.tecngo.users.controller;

import com.tecngo.users.dto.UserVerificationResponse;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.VerificationService;
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
}
