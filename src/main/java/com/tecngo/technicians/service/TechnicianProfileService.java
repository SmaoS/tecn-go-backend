package com.tecngo.technicians.service;

import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.technicians.dto.TechnicianProfileRequest;
import com.tecngo.technicians.dto.TechnicianProfileResponse;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import com.tecngo.users.service.UserService;
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

    @Transactional
    public TechnicianProfileResponse create(TechnicianProfileRequest request, User user) {
        if (profiles.existsByUserId(user.getId())) throw new ConflictException("Technician profile already exists");
        if (profiles.existsByDocumentNumber(request.documentNumber())) {
            throw new ConflictException("Document number is already registered");
        }
        updateUserEvidence(user, request);
        return map(profiles.save(TechnicianProfile.builder()
                .user(user)
                .documentNumber(request.documentNumber().trim())
                .phone(request.phone().trim())
                .categories(categories(request.categoryIds()))
                .description(request.description().trim())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build()));
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
        return map(profile);
    }

    @Transactional(readOnly = true)
    public TechnicianProfile approvedProfile(User user) {
        TechnicianProfile profile = findByUser(user);
        if (profile.getStatus() != TechnicianStatus.APPROVED) {
            throw new IllegalStateException("Technician profile must be approved");
        }
        if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new IllegalStateException("Technician identity must be verified");
        }
        return profile;
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
                user.getVerificationStatus());
    }

    private Set<ServiceCategory> categories(Set<UUID> ids) {
        Set<ServiceCategory> result = new HashSet<>();
        ids.forEach(id -> result.add(categoryService.requireActive(id)));
        return result;
    }

    private void updateUserEvidence(User user, TechnicianProfileRequest request) {
        String previousDocument = user.getDocumentPhotoUrl();
        user.setProfilePhotoUrl(clean(request.profilePhotoUrl()));
        user.setDocumentPhotoUrl(request.documentPhotoUrl().trim());
        user.setCertificatePhotoUrl(clean(request.certificatePhotoUrl()));
        user.setWorkExperienceDescription(request.workExperienceDescription().trim());
        userService.markPendingWhenEvidenceChanges(user, previousDocument, user.getDocumentPhotoUrl());
        users.save(user);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
