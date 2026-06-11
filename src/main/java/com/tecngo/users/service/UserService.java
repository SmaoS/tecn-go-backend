package com.tecngo.users.service;

import com.tecngo.users.dto.UserProfileRequest;
import com.tecngo.users.dto.UserProfileResponse;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;

    @Transactional
    public void updateFcmToken(User user, String token) {
        user.setFcmToken(token.trim());
        user.setFcmTokenUpdatedAt(Instant.now());
        users.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(User user) {
        return map(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, UserProfileRequest request) {
        if (user.getRole() == Role.TECHNICIAN
                && (request.workExperienceDescription() == null
                || request.workExperienceDescription().isBlank())) {
            throw new IllegalArgumentException("Work experience description is required for technicians");
        }
        String previousDocument = clean(user.getDocumentPhotoUrl());
        String newDocument = clean(request.documentPhotoUrl());
        String previousProfilePhoto = clean(user.getProfilePhotoUrl());
        String newProfilePhoto = clean(request.profilePhotoUrl());
        user.setFullName(request.fullName().trim());
        user.setPhone(clean(request.phone()));
        user.setProfilePhotoUrl(newProfilePhoto);
        user.setDocumentPhotoUrl(newDocument);
        user.setCertificatePhotoUrl(clean(request.certificatePhotoUrl()));
        user.setWorkExperienceDescription(clean(request.workExperienceDescription()));
        user.setHomeAddress(clean(request.homeAddress()));
        user.setHomeLatitude(request.homeLatitude());
        user.setHomeLongitude(request.homeLongitude());
        user.setHomeCity(clean(request.homeCity()));
        user.setHomeNeighborhood(clean(request.homeNeighborhood()));
        if (user.getRole() == Role.TECHNICIAN && user.getHomeAddress() == null) {
            throw new IllegalArgumentException("Home address is required for technicians");
        }
        if (!java.util.Objects.equals(previousProfilePhoto, newProfilePhoto)) {
            user.setProfilePhotoFaceValidated(false);
            user.setProfilePhotoVerifiedBy(null);
            user.setProfilePhotoVerifiedAt(null);
        }
        updateVerificationStatus(user, previousDocument, newDocument);
        return map(users.save(user));
    }

    private UserProfileResponse map(User user) {
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(),
                user.getProfilePhotoUrl(), user.getDocumentPhotoUrl(), user.getCertificatePhotoUrl(),
                user.getWorkExperienceDescription(), user.getAverageRating(),
                user.getCompletedServicesCount(), user.getPaidServicesCount(),
                user.getVerificationStatus(), user.isEmailVerified(), user.isPhoneVerified(),
                user.isDocumentsVerified(), user.getHomeAddress(), user.getHomeLatitude(),
                user.getHomeLongitude(), user.getHomeCity(), user.getHomeNeighborhood(),
                user.getAccountStatus(), user.getInactiveReason(), user.getInactiveComment(),
                user.isProfilePhotoFaceValidated());
    }

    public void markPendingWhenEvidenceChanges(User user, String previousDocument, String newDocument) {
        updateVerificationStatus(user, clean(previousDocument), clean(newDocument));
    }

    private void updateVerificationStatus(User user, String previousDocument, String newDocument) {
        if (newDocument == null) {
            user.setVerificationStatus(VerificationStatus.CREATED);
            user.setDocumentsVerified(false);
            user.setVerifiedAt(null);
            user.setVerifiedBy(null);
        } else if (!newDocument.equals(previousDocument)
                || user.getVerificationStatus() == VerificationStatus.CREATED) {
            user.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
            user.setDocumentsVerified(false);
            user.setVerifiedAt(null);
            user.setVerifiedBy(null);
        }
    }

    public String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
