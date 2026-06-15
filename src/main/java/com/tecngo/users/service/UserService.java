package com.tecngo.users.service;

import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.content_moderation.entity.ContentAssetKind;
import com.tecngo.content_moderation.service.ManagedContentPolicy;
import com.tecngo.users.dto.UserProfileRequest;
import com.tecngo.users.dto.UserProfileResponse;
import com.tecngo.users.dto.ChangePasswordRequest;
import com.tecngo.password_recovery.dto.PasswordMessageResponse;
import com.tecngo.password_recovery.entity.PasswordSecurityAudit;
import com.tecngo.password_recovery.repository.PasswordResetTokenRepository;
import com.tecngo.password_recovery.repository.PasswordSecurityAuditRepository;
import com.tecngo.shared.exception.UnauthorizedException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final ManagedContentPolicy managedContent;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final PasswordSecurityAuditRepository passwordAudits;
    private final GeographicCatalogService geographicCatalogs;

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
        String newDocument = managedContent.validateChange(previousDocument,
                request.documentPhotoUrl(), user, Set.of(ContentAssetKind.DOCUMENT));
        String previousProfilePhoto = clean(user.getProfilePhotoUrl());
        String newProfilePhoto = managedContent.validateChange(previousProfilePhoto,
                request.profilePhotoUrl(), user, Set.of(ContentAssetKind.PROFILE));
        String newCertificate = managedContent.validateChange(user.getCertificatePhotoUrl(),
                request.certificatePhotoUrl(), user, Set.of(ContentAssetKind.CERTIFICATE));
        user.setFullName(request.fullName().trim());
        user.setPhone(clean(request.phone()));
        user.setProfilePhotoUrl(newProfilePhoto);
        user.setDocumentPhotoUrl(newDocument);
        user.setCertificatePhotoUrl(newCertificate);
        user.setWorkExperienceDescription(clean(request.workExperienceDescription()));
        user.setHomeAddress(clean(request.homeAddress()));
        user.setHomeLatitude(request.homeLatitude());
        user.setHomeLongitude(request.homeLongitude());
        user.setHomeNeighborhood(clean(request.homeNeighborhood()));
        applyGeographicSelection(user, request.countryId(), request.departmentId(), request.cityId(),
                request.homeCity());
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

    @Transactional
    public PasswordMessageResponse changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("La contraseña actual es incorrecta");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
        passwordResetTokens.invalidateActiveByUserId(user.getId(), Instant.now());
        passwordAudits.save(PasswordSecurityAudit.builder()
                .user(user)
                .action("PASSWORD_CHANGED")
                .build());
        return new PasswordMessageResponse("Contraseña actualizada correctamente.");
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
                user.isProfilePhotoFaceValidated(),
                user.getCountry() == null ? null : user.getCountry().getId(),
                user.getCountry() == null ? null : user.getCountry().getName(),
                user.getDepartment() == null ? null : user.getDepartment().getId(),
                user.getDepartment() == null ? null : user.getDepartment().getName(),
                user.getCity() == null ? null : user.getCity().getId(),
                user.getCity() == null ? null : user.getCity().getName());
    }

    private void applyGeographicSelection(User user, java.util.UUID countryId,
                                          java.util.UUID departmentId, java.util.UUID cityId,
                                          String legacyCity) {
        if (countryId == null && departmentId == null && cityId == null) {
            user.setHomeCity(clean(legacyCity));
            return;
        }
        var selection = geographicCatalogs.requireSelection(countryId, departmentId, cityId);
        user.setCountry(selection.country());
        user.setDepartment(selection.department());
        user.setCity(selection.city());
        user.setHomeCity(selection.city().getName());
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
