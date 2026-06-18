package com.tecngo.users.service;

import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.content_moderation.entity.ContentAssetKind;
import com.tecngo.content_moderation.service.ManagedContentPolicy;
import com.tecngo.legal.service.LegalService;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.dto.*;
import com.tecngo.users.entity.*;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final UserRepository users;
    private final GeographicCatalogService geographicCatalogs;
    private final ManagedContentPolicy managedContent;
    private final LegalService legal;
    private final TechnicianProfileRepository technicianProfiles;
    private final ServiceCategoryService serviceCategories;

    @Transactional(readOnly = true)
    public OnboardingStatusResponse status(User user) {
        OnboardingStep current = currentStep(user);
        return new OnboardingStatusResponse(user.isEmailVerified(), user.isOnboardingCompleted(),
                current, requiredSteps(user), user.isOnboardingCompleted() ? "HOME" : null);
    }

    @Transactional
    public OnboardingStatusResponse mainData(User user, OnboardingMainDataRequest request) {
        requireEmail(user);
        user.setFullName(request.fullName().trim());
        user.setPhone(clean(request.phone()));
        var selection = geographicCatalogs.requireSelection(request.countryId(), request.departmentId(), request.cityId());
        user.setCountry(selection.country());
        user.setDepartment(selection.department());
        user.setCity(selection.city());
        user.setHomeCity(selection.city().getName());
        user.setHomeAddress(request.address().trim());
        user.setHomeNeighborhood(clean(request.neighborhood()));
        user.setDocumentType(request.documentType());
        user.setDocumentNumber(request.documentNumber().trim());
        user.setOnboardingStep(OnboardingStep.LEGAL_ACCEPTANCE);
        users.save(user);
        return status(user);
    }

    @Transactional
    public OnboardingStatusResponse legalAcceptance(User user) {
        requireEmail(user);
        legal.acceptAll(user);
        user.setOnboardingStep(OnboardingStep.PROFILE_SELFIE);
        users.save(user);
        return status(user);
    }

    @Transactional
    public OnboardingStatusResponse profileSelfie(User user, ProfileSelfieRequest request) {
        requireEmail(user);
        if (user.isProfileSelfieLocked()) {
            throw new ConflictException("Profile selfie is already locked");
        }
        String url = managedContent.validateChange(null, request.profilePhotoUrl(), user, Set.of(ContentAssetKind.PROFILE));
        user.setProfilePhotoUrl(url);
        user.setProfileSelfieLocked(true);
        FaceDetectionStatus detectionStatus = request.faceDetectionStatus() == null
                ? FaceDetectionStatus.MANUAL_REVIEW_REQUIRED : request.faceDetectionStatus();
        user.setFaceDetectionStatus(detectionStatus);
        user.setProfilePhotoFaceValidated(detectionStatus == FaceDetectionStatus.AUTO_VALIDATED);
        user.setOnboardingStep(OnboardingStep.IDENTITY_DOCUMENT);
        users.save(user);
        return status(user);
    }

    @Transactional
    public OnboardingStatusResponse identityDocument(User user, IdentityDocumentRequest request) {
        requireEmail(user);
        if (request.documentType() == DocumentType.CC) {
            if (blank(request.documentFrontUrl()) || blank(request.documentBackUrl())) {
                throw new IllegalArgumentException("CC requires front and back document images");
            }
            String front = managedContent.validateChange(user.getDocumentFrontUrl(), request.documentFrontUrl(),
                    user, Set.of(ContentAssetKind.DOCUMENT));
            String back = managedContent.validateChange(user.getDocumentBackUrl(), request.documentBackUrl(),
                    user, Set.of(ContentAssetKind.DOCUMENT));
            user.setDocumentFrontUrl(front);
            user.setDocumentBackUrl(back);
            user.setDocumentSingleUrl(null);
            user.setDocumentPhotoUrl(front);
        } else {
            if (blank(request.documentSingleUrl())) {
                throw new IllegalArgumentException("Passport requires one document image");
            }
            String single = managedContent.validateChange(user.getDocumentSingleUrl(), request.documentSingleUrl(),
                    user, Set.of(ContentAssetKind.DOCUMENT));
            user.setDocumentSingleUrl(single);
            user.setDocumentFrontUrl(null);
            user.setDocumentBackUrl(null);
            user.setDocumentPhotoUrl(single);
        }
        user.setDocumentType(request.documentType());
        user.setIdentityDocumentCaptureStatus(request.identityDocumentCaptureStatus() == null
                ? IdentityDocumentCaptureStatus.MANUAL_REVIEW_REQUIRED : request.identityDocumentCaptureStatus());
        user.setDocumentsVerified(false);
        user.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        if (user.getRole() == Role.TECHNICIAN) {
            user.setOnboardingStep(OnboardingStep.TECHNICIAN_PROFESSIONAL_PROFILE);
        } else {
            markCompleted(user);
        }
        users.save(user);
        return status(user);
    }

    @Transactional
    public OnboardingStatusResponse professionalProfile(
            User user, TechnicianProfessionalProfileRequest request) {
        requireTechnician(user);
        if (request.categoryIds() == null || request.categoryIds().isEmpty()) {
            throw new IllegalArgumentException("At least one service category is required");
        }
        if (blank(request.workExperienceDescription())) {
            throw new IllegalArgumentException("Work experience description is required");
        }
        String experience = request.workExperienceDescription().trim();
        if (experience.length() < 30 || experience.length() > 1000) {
            throw new IllegalArgumentException(
                    "Work experience description must contain between 30 and 1000 characters");
        }
        Set<ServiceCategory> categories = new HashSet<>();
        request.categoryIds().forEach(id -> categories.add(serviceCategories.requireActive(id)));
        TechnicianProfile profile = technicianProfiles.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            if (technicianProfiles.existsByDocumentNumber(user.getDocumentNumber())) {
                throw new ConflictException("Document number is already registered");
            }
            profile = TechnicianProfile.builder()
                    .user(user)
                    .documentNumber(user.getDocumentNumber())
                    .phone(user.getPhone())
                    .build();
        }
        profile.setDocumentNumber(user.getDocumentNumber());
        profile.setPhone(user.getPhone());
        profile.setCategories(categories);
        profile.setDescription(experience);
        technicianProfiles.save(profile);
        user.setWorkExperienceDescription(experience);
        user.setOnboardingStep(OnboardingStep.TECHNICIAN_CERTIFICATE);
        users.save(user);
        return status(user);
    }

    @Transactional
    public OnboardingStatusResponse certificate(User user, CertificateRequest request) {
        requireTechnician(user);
        requireProfessionalProfile(user);
        if (!blank(request.certificateUrl())) {
            user.setCertificatePhotoUrl(managedContent.validateChange(user.getCertificatePhotoUrl(),
                    request.certificateUrl(), user, Set.of(ContentAssetKind.CERTIFICATE)));
        }
        markCompleted(user);
        users.save(user);
        return status(user);
    }

    @Transactional
    public OnboardingStatusResponse skipCertificate(User user) {
        requireTechnician(user);
        requireProfessionalProfile(user);
        markCompleted(user);
        users.save(user);
        return status(user);
    }

    @Transactional
    public OnboardingStatusResponse complete(User user) {
        requireEmail(user);
        OnboardingStep current = currentStep(user);
        if (current != OnboardingStep.COMPLETED) {
            throw new ConflictException("Onboarding step " + current + " is pending");
        }
        user.setOnboardingCompleted(true);
        user.setOnboardingStep(OnboardingStep.COMPLETED);
        users.save(user);
        return status(user);
    }

    @Transactional
    public OnboardingStatusResponse autoComplete(User user) {
        requireEmail(user);
        if (currentStep(user) != OnboardingStep.COMPLETED) {
            throw new ConflictException("Onboarding step " + currentStep(user) + " is pending");
        }
        markCompleted(user);
        users.save(user);
        return status(user);
    }

    private OnboardingStep currentStep(User user) {
        if (!user.isEmailVerified()) return OnboardingStep.MAIN_DATA;
        if (blank(user.getPhone()) || user.getCity() == null || blank(user.getHomeAddress())
                || user.getDocumentType() == null || blank(user.getDocumentNumber())) return OnboardingStep.MAIN_DATA;
        if (!legal.status(user).complete()) return OnboardingStep.LEGAL_ACCEPTANCE;
        if (blank(user.getProfilePhotoUrl())) return OnboardingStep.PROFILE_SELFIE;
        if (user.getDocumentType() == DocumentType.CC
                && (blank(user.getDocumentFrontUrl()) || blank(user.getDocumentBackUrl()))) {
            return OnboardingStep.IDENTITY_DOCUMENT;
        }
        if (user.getDocumentType() == DocumentType.PASSPORT && blank(user.getDocumentSingleUrl())) {
            return OnboardingStep.IDENTITY_DOCUMENT;
        }
        if (user.getRole() == Role.TECHNICIAN && !hasProfessionalProfile(user)) {
            return OnboardingStep.TECHNICIAN_PROFESSIONAL_PROFILE;
        }
        if (user.getRole() == Role.TECHNICIAN && user.getOnboardingStep() == OnboardingStep.TECHNICIAN_CERTIFICATE) {
            return OnboardingStep.TECHNICIAN_CERTIFICATE;
        }
        return OnboardingStep.COMPLETED;
    }

    private List<OnboardingStep> requiredSteps(User user) {
        List<OnboardingStep> steps = new ArrayList<>(List.of(OnboardingStep.MAIN_DATA,
                OnboardingStep.LEGAL_ACCEPTANCE, OnboardingStep.PROFILE_SELFIE, OnboardingStep.IDENTITY_DOCUMENT));
        if (user.getRole() == Role.TECHNICIAN) {
            steps.add(OnboardingStep.TECHNICIAN_PROFESSIONAL_PROFILE);
            steps.add(OnboardingStep.TECHNICIAN_CERTIFICATE);
        }
        return steps;
    }

    private boolean hasProfessionalProfile(User user) {
        return technicianProfiles.findByUserId(user.getId())
                .map(profile -> !profile.getCategories().isEmpty()
                        && !blank(profile.getDescription())
                        && profile.getDescription().trim().length() >= 30
                        && !blank(user.getWorkExperienceDescription())
                        && user.getWorkExperienceDescription().trim().length() >= 30)
                .orElse(false);
    }

    private void requireProfessionalProfile(User user) {
        if (!hasProfessionalProfile(user)) {
            throw new ConflictException("Technician professional profile is pending");
        }
    }

    private void markCompleted(User user) {
        user.setOnboardingCompleted(true);
        user.setOnboardingStep(OnboardingStep.COMPLETED);
    }

    private void requireEmail(User user) {
        if (!user.isEmailVerified()) throw new ForbiddenException("Email verification is required");
    }

    private void requireTechnician(User user) {
        requireEmail(user);
        if (user.getRole() != Role.TECHNICIAN) throw new ForbiddenException("Technician role is required");
    }

    private String clean(String value) { return blank(value) ? null : value.trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
