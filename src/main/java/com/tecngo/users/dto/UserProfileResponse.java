package com.tecngo.users.dto;

import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.InactivationReason;

import java.math.BigDecimal;
import java.util.UUID;

public record UserProfileResponse(
        UUID id, String fullName, String email, String phone, Role role, String profilePhotoUrl,
        String documentPhotoUrl, String certificatePhotoUrl, String workExperienceDescription,
        BigDecimal averageRating, long completedServicesCount, long paidServicesCount,
        VerificationStatus verificationStatus, boolean emailVerified, boolean phoneVerified,
        boolean documentsVerified, String homeAddress, Double homeLatitude, Double homeLongitude,
        String homeCity, String homeNeighborhood, AccountStatus accountStatus,
        InactivationReason inactiveReason, String inactiveComment,
        boolean profilePhotoFaceValidated, UUID countryId, String countryName,
        UUID departmentId, String departmentName, UUID cityId, String cityName
) {}
