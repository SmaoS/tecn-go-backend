package com.tecngo.users.service;

import com.tecngo.users.dto.UserProfileRequest;
import com.tecngo.users.dto.UserProfileResponse;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;

    @Transactional
    public void updateFcmToken(User user, String token) {
        user.setFcmToken(token.trim());
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
        user.setFullName(request.fullName().trim());
        user.setProfilePhotoUrl(clean(request.profilePhotoUrl()));
        user.setDocumentPhotoUrl(request.documentPhotoUrl().trim());
        user.setCertificatePhotoUrl(clean(request.certificatePhotoUrl()));
        user.setWorkExperienceDescription(clean(request.workExperienceDescription()));
        return map(users.save(user));
    }

    private UserProfileResponse map(User user) {
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getProfilePhotoUrl(), user.getDocumentPhotoUrl(), user.getCertificatePhotoUrl(),
                user.getWorkExperienceDescription(), user.getAverageRating(),
                user.getCompletedServicesCount(), user.getPaidServicesCount());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
