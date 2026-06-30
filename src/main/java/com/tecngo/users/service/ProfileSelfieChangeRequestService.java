package com.tecngo.users.service;

import com.tecngo.content_moderation.entity.ContentAssetKind;
import com.tecngo.content_moderation.entity.ModerationStatus;
import com.tecngo.content_moderation.repository.ContentAssetRepository;
import com.tecngo.content_moderation.service.ManagedContentPolicy;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.dto.ProfileSelfieChangeRequestCreate;
import com.tecngo.users.dto.ProfileSelfieChangeRequestResponse;
import com.tecngo.users.entity.FaceDetectionStatus;
import com.tecngo.users.entity.ProfileSelfieChangeRequest;
import com.tecngo.users.entity.ProfileSelfieChangeRequestStatus;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.ProfileSelfieChangeRequestRepository;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileSelfieChangeRequestService {
    private final ProfileSelfieChangeRequestRepository requests;
    private final UserRepository users;
    private final ManagedContentPolicy managedContent;
    private final ContentAssetRepository contentAssets;

    @Transactional
    public ProfileSelfieChangeRequestResponse create(User principal, ProfileSelfieChangeRequestCreate request) {
        User user = users.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (requests.existsByUserIdAndStatus(user.getId(), ProfileSelfieChangeRequestStatus.PENDING)) {
            throw new ConflictException("Ya tienes una solicitud de cambio de selfie pendiente");
        }
        String proposedPhoto = managedContent.validateChange(
                null,
                request.profilePhotoUrl(),
                user,
                Set.of(ContentAssetKind.PROFILE)
        );
        if (proposedPhoto == null || proposedPhoto.isBlank()) {
            throw new IllegalArgumentException("La selfie propuesta es obligatoria");
        }
        ProfileSelfieChangeRequest saved = requests.save(ProfileSelfieChangeRequest.builder()
                .user(user)
                .currentPhotoUrl(user.getProfilePhotoUrl())
                .requestedPhotoUrl(proposedPhoto)
                .faceDetectionStatus(request.faceDetectionStatus())
                .status(ProfileSelfieChangeRequestStatus.PENDING)
                .build());
        return map(saved);
    }

    @Transactional(readOnly = true)
    public List<ProfileSelfieChangeRequestResponse> mine(User user) {
        return requests.findByUserIdOrderByRequestedAtDesc(user.getId()).stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfileSelfieChangeRequestResponse> pending() {
        return requests.findByStatusOrderByRequestedAtAsc(ProfileSelfieChangeRequestStatus.PENDING).stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public ProfileSelfieChangeRequestResponse approve(UUID id, User reviewer) {
        ProfileSelfieChangeRequest request = findPending(id);
        User user = request.getUser();
        user.setProfilePhotoUrl(request.getRequestedPhotoUrl());
        user.setProfilePhotoFaceValidated(true);
        user.setFaceDetectionStatus(FaceDetectionStatus.AUTO_VALIDATED);
        user.setProfileSelfieLocked(true);
        user.setProfilePhotoVerifiedBy(reviewer);
        user.setProfilePhotoVerifiedAt(Instant.now());
        approveContentAsset(request.getRequestedPhotoUrl(), user, reviewer);
        request.setStatus(ProfileSelfieChangeRequestStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(Instant.now());
        return map(requests.save(request));
    }

    @Transactional
    public ProfileSelfieChangeRequestResponse reject(UUID id, String reason, User reviewer) {
        ProfileSelfieChangeRequest request = findPending(id);
        request.setStatus(ProfileSelfieChangeRequestStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(Instant.now());
        request.setRejectionReason(reason);
        return map(requests.save(request));
    }

    private ProfileSelfieChangeRequest findPending(UUID id) {
        ProfileSelfieChangeRequest request = requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Profile selfie change request not found"));
        if (request.getStatus() != ProfileSelfieChangeRequestStatus.PENDING) {
            throw new ConflictException("La solicitud ya fue revisada");
        }
        return request;
    }

    private void approveContentAsset(String url, User owner, User reviewer) {
        contentAssets.findByFileUrlAndUploadedById(url, owner.getId())
                .filter(asset -> asset.getModerationStatus() != ModerationStatus.REJECTED)
                .ifPresent(asset -> {
                    asset.setModerationStatus(ModerationStatus.APPROVED);
                    asset.setModerationReason("Profile selfie change approved by verifier");
                    asset.setModeratedAt(Instant.now());
                    asset.setModeratedBy(reviewer);
                });
    }

    private ProfileSelfieChangeRequestResponse map(ProfileSelfieChangeRequest request) {
        User user = request.getUser();
        return new ProfileSelfieChangeRequestResponse(
                request.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                request.getCurrentPhotoUrl(),
                request.getRequestedPhotoUrl(),
                request.getFaceDetectionStatus(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getReviewedBy() == null ? null : request.getReviewedBy().getId(),
                request.getReviewedAt(),
                request.getRejectionReason()
        );
    }
}
