package com.tecngo.technicians.service;

import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.content_moderation.entity.ContentAssetKind;
import com.tecngo.content_moderation.service.ManagedContentPolicy;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.CodedForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.technicians.dto.TechnicianProfileRequest;
import com.tecngo.technicians.dto.TechnicianProfileResponse;
import com.tecngo.technicians.dto.TechnicianAvailabilityResponse;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.users.service.UserService;
import com.tecngo.verification.service.EmailVerificationService;
import com.tecngo.referrals.service.ReferralService;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TechnicianProfileService {
    private final TechnicianProfileRepository profiles;
    private final ServiceCategoryService categoryService;
    private final UserRepository users;
    private final UserService userService;
    private final EmailVerificationService emailVerification;
    private final ReferralService referrals;
    private final ManagedContentPolicy managedContent;
    private final GeographicCatalogService geographicCatalogs;
    private final TechnicianWalletService wallets;

    @Transactional
    public TechnicianProfileResponse create(TechnicianProfileRequest request, User user) {
        if (profiles.existsByUserId(user.getId())) throw new ConflictException("Technician profile already exists");
        if (profiles.existsByDocumentNumber(request.documentNumber())) {
            throw new ConflictException("Document number is already registered");
        }
        updateUserEvidence(user, request);
        TechnicianProfile saved = profiles.save(TechnicianProfile.builder()
                .user(user)
                .documentNumber(request.documentNumber().trim())
                .phone(request.phone().trim())
                .categories(categories(request.categoryIds()))
                .description(request.description().trim())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build());
        wallets.ensureWallet(user);
        referrals.ensureCode(user);
        return map(saved);
    }

    @Transactional(readOnly = true)
    public TechnicianProfileResponse mine(User user) {
        return map(findByUser(user));
    }

    @Transactional
    public TechnicianProfileResponse update(TechnicianProfileRequest request, User user) {
        TechnicianProfile profile = findByUser(user);
        profile.setPhone(request.phone().trim());
        profile.setCategories(categories(request.categoryIds()));
        profile.setDescription(request.description().trim());
        profile.setLatitude(request.latitude());
        profile.setLongitude(request.longitude());
        updateUserEvidence(user, request);
        if (!profile.getDocumentNumber().equals(request.documentNumber())) {
            if (profiles.existsByDocumentNumber(request.documentNumber())) {
                throw new ConflictException("Document number is already registered");
            }
            profile.setDocumentNumber(request.documentNumber().trim());
        }
        if (profile.getStatus() == TechnicianStatus.REJECTED) profile.setStatus(TechnicianStatus.PENDING);
        return map(profile);
    }

    @Transactional(readOnly = true)
    public TechnicianAvailabilityResponse availability(User user) {
        return new TechnicianAvailabilityResponse(findByUser(user).isAvailable());
    }

    @Transactional
    public TechnicianAvailabilityResponse updateAvailability(User user, boolean available) {
        TechnicianProfile profile = findByUser(user);
        if (available) requireOperationalProfile(user, profile);
        profile.setAvailable(available);
        return new TechnicianAvailabilityResponse(available);
    }

    @Transactional(readOnly = true)
    public List<TechnicianProfileResponse> pending() {
        return profiles.findByStatusOrderByCreatedAtAsc(TechnicianStatus.PENDING).stream().map(this::map).toList();
    }

    @Transactional
    public TechnicianProfileResponse review(UUID id, TechnicianStatus status) {
        TechnicianProfile profile = profiles.findById(id)
                .orElseThrow(() -> new NotFoundException("Technician profile not found"));
        if (status == TechnicianStatus.APPROVED
                && profile.getUser().getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("User identity must be verified first");
        }
        profile.setStatus(status);
        if (status == TechnicianStatus.APPROVED) {
            referrals.ensureCode(profile.getUser());
            wallets.ensureWallet(profile.getUser());
        }
        return map(profile);
    }

    @Transactional(readOnly = true)
    public TechnicianProfile approvedProfile(User user) {
        TechnicianProfile profile = findByUser(user);
        requireOperationalProfile(user, profile);
        if (profile.getStatus() != TechnicianStatus.APPROVED) {
            throw new IllegalStateException("Technician profile must be approved");
        }
        if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Technician identity must be verified");
        }
        return profile;
    }

    public void requireOperationalProfile(User user, TechnicianProfile profile) {
        boolean incomplete = !user.isEmailVerified()
                || !user.isOnboardingCompleted()
                || user.getDocumentPhotoUrl() == null
                || user.getDocumentPhotoUrl().isBlank()
                || user.getWorkExperienceDescription() == null
                || user.getWorkExperienceDescription().trim().length() < 30
                || profile.getDescription() == null
                || profile.getDescription().trim().length() < 30
                || profile.getCategories() == null
                || profile.getCategories().isEmpty();
        if (incomplete) {
            throw new CodedForbiddenException("TECHNICIAN_PROFILE_INCOMPLETE",
                    "Completa tu perfil técnico para poder operar.");
        }
    }

    @Transactional(readOnly = true)
    public List<String> categoryNames(User user) {
        return profiles.findByUserId(user.getId())
                .map(profile -> profile.getCategories().stream()
                        .map(ServiceCategory::getName)
                        .sorted()
                        .toList())
                .orElseGet(List::of);
    }

    private TechnicianProfile findByUser(User user) {
        return profiles.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Technician profile not found"));
    }

    private TechnicianProfileResponse map(TechnicianProfile profile) {
        User user = profile.getUser();
        return new TechnicianProfileResponse(profile.getId(), user.getId(), user.getFullName(), user.getEmail(),
                profile.getDocumentNumber(), profile.getPhone(),
                profile.getCategories().stream().map(categoryService::map).toList(), profile.getDescription(),
                profile.getLatitude(), profile.getLongitude(), profile.getStatus(), user.getProfilePhotoUrl(),
                user.getDocumentPhotoUrl(), user.getCertificatePhotoUrl(), user.getWorkExperienceDescription(),
                user.getAverageRating(), user.getCompletedServicesCount(), user.getPaidServicesCount(),
                user.getVerificationStatus(), user.getHomeAddress(), user.getHomeLatitude(),
                user.getHomeLongitude(), user.getHomeCity(), user.getHomeNeighborhood(),
                user.getCountry() == null ? null : user.getCountry().getId(),
                user.getCountry() == null ? null : user.getCountry().getName(),
                user.getDepartment() == null ? null : user.getDepartment().getId(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.getCity() == null ? null : user.getCity().getId(),
                user.getCity() == null ? null : user.getCity().getName());
    }

    private Set<ServiceCategory> categories(Set<UUID> ids) {
        Set<ServiceCategory> result = new HashSet<>();
        ids.forEach(id -> result.add(categoryService.requireActive(id)));
        return result;
    }

    private void updateUserEvidence(User user, TechnicianProfileRequest request) {
        String previousDocument = user.getDocumentPhotoUrl();
        user.setProfilePhotoUrl(managedContent.validateChange(user.getProfilePhotoUrl(),
                request.profilePhotoUrl(), user, Set.of(ContentAssetKind.PROFILE)));
        user.setDocumentPhotoUrl(managedContent.validateChange(previousDocument,
                request.documentPhotoUrl(), user, Set.of(ContentAssetKind.DOCUMENT)));
        user.setCertificatePhotoUrl(managedContent.validateChange(user.getCertificatePhotoUrl(),
                request.certificatePhotoUrl(), user, Set.of(ContentAssetKind.CERTIFICATE)));
        user.setWorkExperienceDescription(request.workExperienceDescription().trim());
        user.setHomeAddress(request.homeAddress().trim());
        user.setHomeLatitude(request.homeLatitude());
        user.setHomeLongitude(request.homeLongitude());
        user.setHomeNeighborhood(clean(request.homeNeighborhood()));
        if (request.countryId() == null && request.departmentId() == null && request.cityId() == null) {
            user.setHomeCity(clean(request.homeCity()));
        } else {
            var selection = geographicCatalogs.requireSelection(
                    request.countryId(), request.departmentId(), request.cityId());
            user.setCountry(selection.country());
            user.setDepartment(selection.department());
            user.setCity(selection.city());
            user.setHomeCity(selection.city().getName());
        }
        userService.markPendingWhenEvidenceChanges(user, previousDocument, user.getDocumentPhotoUrl());
        users.save(user);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
