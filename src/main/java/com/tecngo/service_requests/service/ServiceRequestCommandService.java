package com.tecngo.service_requests.service;

import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.service_requests.dto.CreateServiceRequest;
import com.tecngo.service_requests.dto.ServiceRequestResponse;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.verification.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceRequestCommandService {
    private final ServiceRequestRepository requests;
    private final ServiceCategoryService categories;
    private final GeographicCatalogService geographicCatalogs;
    private final EmailVerificationService emailVerification;
    private final ServiceRequestAccessPolicy access;
    private final ServiceRequestNotifier notifier;
    private final ServiceRequestAssembler assembler;

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequest request, User client) {
        access.requireRole(client, Role.CLIENT);
        access.requireCriticalAccess(client);
        emailVerification.requireVerified(client);
        if (client.getDocumentPhotoUrl() == null || client.getDocumentPhotoUrl().isBlank()) {
            throw new ConflictException("Complete your profile with a document before requesting a service");
        }
        var category = categories.requireActive(request.categoryId());
        var city = request.cityId() != null
                ? geographicCatalogs.requireCity(request.cityId())
                : client.getCity();
        ServiceRequest saved = requests.save(ServiceRequest.builder()
                .client(client)
                .category(category)
                .city(city)
                .description(request.description())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .estimatedPrice(request.estimatedPrice())
                .requestedPaymentMethod(request.paymentMethod() == null ? PaymentMethod.CASH : request.paymentMethod())
                .build());
        notifier.nearbyTechnicians(saved);
        return assembler.response(saved);
    }
}
