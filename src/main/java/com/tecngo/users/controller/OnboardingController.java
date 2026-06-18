package com.tecngo.users.controller;

import com.tecngo.users.dto.*;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService service;

    @GetMapping("/v1/users/me/onboarding-status")
    public OnboardingStatusResponse status(@AuthenticationPrincipal User user) {
        return service.status(user);
    }

    @PutMapping("/v1/users/me/onboarding/main-data")
    public OnboardingStatusResponse mainData(@AuthenticationPrincipal User user,
                                             @Valid @RequestBody OnboardingMainDataRequest request) {
        return service.mainData(user, request);
    }

    @PostMapping("/v1/users/me/onboarding/legal-acceptance")
    public OnboardingStatusResponse legal(@AuthenticationPrincipal User user) {
        return service.legalAcceptance(user);
    }

    @PostMapping("/v1/users/me/onboarding/profile-selfie")
    public OnboardingStatusResponse selfie(@AuthenticationPrincipal User user,
                                           @Valid @RequestBody ProfileSelfieRequest request) {
        return service.profileSelfie(user, request);
    }

    @PostMapping("/v1/users/me/onboarding/identity-document")
    public OnboardingStatusResponse document(@AuthenticationPrincipal User user,
                                             @Valid @RequestBody IdentityDocumentRequest request) {
        return service.identityDocument(user, request);
    }

    @PutMapping("/v1/technicians/me/onboarding/professional-profile")
    public OnboardingStatusResponse professionalProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TechnicianProfessionalProfileRequest request) {
        return service.professionalProfile(user, request);
    }

    @PutMapping("/v1/users/me/onboarding/complete")
    public OnboardingStatusResponse complete(@AuthenticationPrincipal User user) {
        return service.complete(user);
    }

    @PostMapping("/v1/users/me/onboarding/auto-complete")
    public OnboardingStatusResponse autoComplete(@AuthenticationPrincipal User user) {
        return service.autoComplete(user);
    }

    @PostMapping("/v1/technicians/me/onboarding/certificate")
    public OnboardingStatusResponse certificate(@AuthenticationPrincipal User user,
                                                @RequestBody CertificateRequest request) {
        return service.certificate(user, request);
    }

    @PostMapping("/v1/technicians/me/onboarding/skip-certificate")
    public OnboardingStatusResponse skipCertificate(@AuthenticationPrincipal User user) {
        return service.skipCertificate(user);
    }
}
