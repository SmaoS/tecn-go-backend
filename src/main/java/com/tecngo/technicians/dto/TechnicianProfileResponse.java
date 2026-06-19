package com.tecngo.technicians.dto;

import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.services.dto.ServiceCategoryResponse;
import com.tecngo.users.entity.VerificationStatus;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public record TechnicianProfileResponse(
        UUID id, UUID userId, String fullName, String email, String documentNumber,
        String phone, List<ServiceCategoryResponse> categories, String description, Double latitude,
        Double longitude, TechnicianStatus status, String profilePhotoUrl, String documentPhotoUrl,
        boolean profilePhotoFaceValidated, String certificatePhotoUrl,
        String workExperienceDescription, BigDecimal averageRating,
        long completedServicesCount, long paidServicesCount, VerificationStatus verificationStatus,
        String homeAddress, Double homeLatitude, Double homeLongitude, String homeCity,
        String homeNeighborhood, UUID countryId, String countryName, UUID departmentId,
        String departmentName, UUID cityId, String cityName
) {}
