package com.tecngo.users.service;

import com.tecngo.content_moderation.entity.ModerationStatus;
import com.tecngo.content_moderation.repository.ContentAssetRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.users.dto.UserVerificationResponse;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final UserRepository users;
    private final ContentAssetRepository contentAssets;

    @Transactional(readOnly = true)
    public List<UserVerificationResponse> pending() {
        return users.findByVerificationStatusOrderByCreatedAtAsc(VerificationStatus.PENDING_VERIFICATION)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public UserVerificationResponse verify(UUID userId, User reviewer) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getVerificationStatus() != VerificationStatus.PENDING_VERIFICATION) {
            throw new ConflictException("User is not pending verification");
        }
        if (user.getDocumentPhotoUrl() == null || user.getDocumentPhotoUrl().isBlank()) {
            throw new IllegalArgumentException("Document photo is required");
        }
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        user.setDocumentsVerified(true);
        user.setVerifiedAt(Instant.now());
        user.setVerifiedBy(reviewer);
        return map(users.save(user));
    }

    @Transactional
    public UserVerificationResponse reject(UUID userId, User reviewer) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getVerificationStatus() != VerificationStatus.PENDING_VERIFICATION) {
            throw new ConflictException("User is not pending verification");
        }
        user.setVerificationStatus(VerificationStatus.CREATED);
        user.setDocumentsVerified(false);
        user.setVerifiedAt(null);
        user.setVerifiedBy(reviewer);
        return map(users.save(user));
    }

    @Transactional
    public UserVerificationResponse verifyProfilePhoto(UUID userId, User reviewer) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getProfilePhotoUrl() == null || user.getProfilePhotoUrl().isBlank()) {
            throw new ConflictException("User does not have a profile photo");
        }
        user.setProfilePhotoFaceValidated(true);
        user.setProfilePhotoVerifiedBy(reviewer);
        user.setProfilePhotoVerifiedAt(Instant.now());
        contentAssets.findByFileUrlAndUploadedById(user.getProfilePhotoUrl(), user.getId())
                .filter(asset -> asset.getModerationStatus() != ModerationStatus.REJECTED)
                .ifPresent(asset -> {
                    asset.setModerationStatus(ModerationStatus.APPROVED);
                    asset.setModerationReason("Profile photo approved by verifier");
                    asset.setModeratedAt(Instant.now());
                    asset.setModeratedBy(reviewer);
                });
        return map(users.save(user));
    }

    private UserVerificationResponse map(User user) {
        return new UserVerificationResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getVerificationStatus(),
                user.getProfilePhotoUrl(),
                user.getDocumentPhotoUrl(),
                user.getCertificatePhotoUrl(),
                user.getWorkExperienceDescription(),
                user.getCreatedAt(),
                user.getVerifiedAt()
        );
    }
}
