package com.tecngo.service_requests.service;

import com.tecngo.service_requests.dto.*;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.geolocation.HaversineDistance;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.verification.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository requests;
    private final ServiceCategoryService categories;
    private final TechnicianProfileService technicianProfiles;
    private final HaversineDistance distance;
    private final ApplicationEventPublisher events;
    private final EmailVerificationService emailVerification;

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequest request, User client) {
        requireRole(client, Role.CLIENT);
        emailVerification.requireVerified(client);
        if (client.getDocumentPhotoUrl() == null || client.getDocumentPhotoUrl().isBlank()) {
            throw new ConflictException("Complete your profile with a document before requesting a service");
        }
        var category = categories.requireActive(request.categoryId());
        return map(requests.save(ServiceRequest.builder()
                .client(client)
                .category(category)
                .description(request.description())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .estimatedPrice(request.estimatedPrice())
                .build()));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> mine(User user) {
        if (user.getRole() == Role.CLIENT) {
            return requests.findByClientIdOrderByCreatedAtDesc(user.getId()).stream().map(this::map).toList();
        }
        if (user.getRole() == Role.TECHNICIAN) {
            return requests.findByTechnicianIdOrderByCreatedAtDesc(user.getId()).stream().map(this::map).toList();
        }
        throw new ForbiddenException("This role does not own service requests");
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> available(User technician, double radiusKm) {
        requireRole(technician, Role.TECHNICIAN);
        emailVerification.requireVerified(technician);
        if (radiusKm <= 0 || radiusKm > 100) {
            throw new IllegalArgumentException("radiusKm must be greater than 0 and at most 100");
        }
        var profile = technicianProfiles.approvedProfile(technician);
        if (profile.getLatitude() == null || profile.getLongitude() == null) {
            throw new ConflictException("Technician location is required");
        }
        List<UUID> categoryIds = profile.getCategories().stream().map(item -> item.getId()).toList();
        return requests.findAvailable(RequestStatus.QUOTE_PENDING, categoryIds).stream()
                .map(item -> map(item, distance.kilometers(profile.getLatitude(), profile.getLongitude(),
                        item.getLatitude(), item.getLongitude()), true))
                .filter(item -> item.distanceKm() <= radiusKm)
                .sorted(Comparator.comparing(ServiceRequestResponse::distanceKm))
                .toList();
    }

    @Transactional
    public ServiceRequestResponse quote(UUID id, BigDecimal technicianPrice, User technician) {
        requireRole(technician, Role.TECHNICIAN);
        emailVerification.requireVerified(technician);
        var profile = technicianProfiles.approvedProfile(technician);
        ServiceRequest request = requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        if (request.getStatus() != RequestStatus.QUOTE_PENDING || request.getTechnician() != null) {
            throw new ConflictException("Service request is no longer available");
        }
        boolean supportsCategory = profile.getCategories().stream()
                .anyMatch(category -> category.getId().equals(request.getCategory().getId()));
        if (!supportsCategory) throw new ForbiddenException("Technician does not support this category");
        request.setTechnician(technician);
        request.setTechnicianPrice(technicianPrice);
        request.setStatus(RequestStatus.QUOTED);
        events.publishEvent(new UserNotificationEvent(request.getClient().getId(), "Nueva cotización",
                technician.getFullName() + " cotizó tu solicitud por $" + technicianPrice,
                NotificationType.QUOTE_RECEIVED));
        return map(request);
    }

    @Transactional
    public ServiceRequestResponse confirmQuote(UUID id, User client) {
        requireRole(client, Role.CLIENT);
        ServiceRequest request = requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        requireClientOwner(request, client);
        if (request.getStatus() != RequestStatus.QUOTED || request.getTechnician() == null
                || request.getTechnicianPrice() == null) {
            throw new ConflictException("Service request does not have a pending quote");
        }
        request.setFinalPrice(request.getTechnicianPrice());
        request.setStatus(RequestStatus.QUOTE_ACCEPTED);
        events.publishEvent(new UserNotificationEvent(request.getTechnician().getId(), "Cotización aceptada",
                request.getClient().getFullName() + " aceptó tu cotización",
                NotificationType.QUOTE_ACCEPTED));
        return map(request);
    }

    @Transactional
    public ServiceRequestResponse updateStatus(UUID id, RequestStatus nextStatus, User user) {
        ServiceRequest request = find(id);
        if (nextStatus == RequestStatus.CANCELLED && user.getRole() == Role.CLIENT) {
            requireClientOwner(request, user);
            if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.PAID
                    || request.getStatus() == RequestStatus.CANCELLED) {
                throw new ConflictException("Completed, paid or cancelled requests cannot be cancelled");
            }
            request.setStatus(RequestStatus.CANCELLED);
            notifyCounterpart(request, user, "Servicio cancelado",
                    "La solicitud fue cancelada", NotificationType.SERVICE_STATUS_CHANGED);
            return map(request);
        }

        requireRole(user, Role.TECHNICIAN);
        requireAssignedTechnician(request, user);
        boolean valid = switch (request.getStatus()) {
            case QUOTE_ACCEPTED -> nextStatus == RequestStatus.ON_THE_WAY;
            case ON_THE_WAY -> nextStatus == RequestStatus.ARRIVED;
            case ARRIVED -> nextStatus == RequestStatus.IN_PROGRESS;
            case IN_PROGRESS -> nextStatus == RequestStatus.COMPLETED;
            default -> false;
        };
        if (!valid) throw new ConflictException("Invalid service request status transition");
        request.setStatus(nextStatus);
        if (nextStatus == RequestStatus.COMPLETED) {
            request.getClient().setCompletedServicesCount(request.getClient().getCompletedServicesCount() + 1);
            request.getTechnician().setCompletedServicesCount(request.getTechnician().getCompletedServicesCount() + 1);
        }
        events.publishEvent(new UserNotificationEvent(request.getClient().getId(), "Estado actualizado",
                "Tu servicio cambió a " + nextStatus, NotificationType.SERVICE_STATUS_CHANGED));
        return map(request);
    }

    private ServiceRequestResponse map(ServiceRequest item) {
        return map(item, null);
    }

    private ServiceRequestResponse map(ServiceRequest item, Double distanceKm) {
        return map(item, distanceKm, false);
    }

    private ServiceRequestResponse map(ServiceRequest item, Double distanceKm, boolean approximateLocation) {
        User technician = item.getTechnician();
        return new ServiceRequestResponse(item.getId(), item.getClient().getId(), item.getClient().getFullName(),
                technician == null ? null : technician.getId(), technician == null ? null : technician.getFullName(),
                item.getClient().getProfilePhotoUrl(), item.getClient().getAverageRating(),
                item.getClient().getPaidServicesCount(),
                technician == null ? null : technician.getProfilePhotoUrl(),
                technician == null ? null : technician.getAverageRating(),
                technician == null ? 0 : technician.getCompletedServicesCount(),
                technician == null ? null : technician.getWorkExperienceDescription(),
                technician == null ? List.of() : technicianProfiles.categoryNames(technician),
                item.getCategory().getId(), item.getCategory().getName(), item.getDescription(),
                approximateLocation ? approximate(item.getAddress()) : item.getAddress(),
                approximateLocation ? null : item.getLatitude(), approximateLocation ? null : item.getLongitude(),
                distanceKm, item.getEstimatedPrice(),
                item.getTechnicianPrice(), item.getFinalPrice(), item.getStatus(), item.getCreatedAt());
    }

    private String approximate(String address) {
        String[] parts = address.split(",");
        if (parts.length < 2) return "Zona cercana";
        int start = Math.max(1, parts.length - 2);
        return String.join(", ", java.util.Arrays.stream(parts, start, parts.length)
                .map(String::trim).toList());
    }

    private ServiceRequest find(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Service request not found"));
    }

    private void requireRole(User user, Role role) {
        if (user.getRole() != role) throw new ForbiddenException("Role " + role + " is required");
    }

    private void requireClientOwner(ServiceRequest request, User user) {
        if (!request.getClient().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the client owner can modify this request");
        }
    }

    private void requireAssignedTechnician(ServiceRequest request, User user) {
        if (request.getTechnician() == null || !request.getTechnician().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the assigned technician can update this request");
        }
    }

    private void notifyCounterpart(ServiceRequest request, User actor, String title, String message,
                                   NotificationType type) {
        User recipient = request.getClient().getId().equals(actor.getId())
                ? request.getTechnician() : request.getClient();
        if (recipient != null) {
            events.publishEvent(new UserNotificationEvent(recipient.getId(), title, message, type));
        }
    }
}
