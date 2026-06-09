package com.tecngo.users.dto;

import com.tecngo.users.entity.Role;

import java.math.BigDecimal;
import java.util.UUID;

public record UserProfileResponse(
        UUID id, String fullName, String email, Role role, String profilePhotoUrl,
        String documentPhotoUrl, String certificatePhotoUrl, String workExperienceDescription,
        BigDecimal averageRating, long completedServicesCount, long paidServicesCount
) {}
