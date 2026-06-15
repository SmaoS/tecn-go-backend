package com.tecngo.service_requests.service;

import com.tecngo.service_requests.dto.*;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.service_requests.repository.ServiceQuoteRepository;
import com.tecngo.service_requests.repository.ServiceRequestImageRepository;
import com.tecngo.service_requests.entity.QuoteStatus;
import com.tecngo.service_requests.entity.ServiceQuote;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.geolocation.HaversineDistance;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.UserAccessService;
import com.tecngo.legal.service.LegalService;
import com.tecngo.verification.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_location.repository.TechnicianLocationRepository;
import com.tecngo.content_moderation.entity.ModerationStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private static final Set<RequestStatus> CLIENT_ACTIVE_STATUSES = Set.of(
            RequestStatus.QUOTE_PENDING, RequestStatus.QUOTED, RequestStatus.QUOTE_ACCEPTED,
            RequestStatus.ON_THE_WAY, RequestStatus.ARRIVED, RequestStatus.IN_PROGRESS,
            RequestStatus.COMPLETED);
    private static final Set<RequestStatus> TECHNICIAN_ACTIVE_STATUSES = Set.of(
            RequestStatus.QUOTE_ACCEPTED, RequestStatus.ON_THE_WAY, RequestStatus.ARRIVED,
            RequestStatus.IN_PROGRESS, RequestStatus.COMPLETED);
    private static final Set<RequestStatus> HISTORY_STATUSES = Set.of(
            RequestStatus.PAID, RequestStatus.CANCELLED);
    private final ServiceRequestRepository requests;
    private final ServiceQuoteRepository quotes;
    private final ServiceRequestImageRepository images;
    private final ServiceCategoryService categories;
    private final TechnicianProfileService technicianProfiles;
    private final TechnicianProfileRepository technicianProfileRepository;
    private final HaversineDistance distance;
    private final ApplicationEventPublisher events;
    private final EmailVerificationService emailVerification;
    private final SystemParameterService parameters;
    private final TechnicianLocationRepository technicianLocations;
    private final UserAccessService userAccess;
    private final LegalService legal;
    @Value("${app.notifications.new-request-radius-km:25}")
    private double newRequestRadiusKm;

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequest request, User client) {
        requireRole(client, Role.CLIENT);
        requireCriticalAccess(client);
        emailVerification.requireVerified(client);
        if (client.getDocumentPhotoUrl() == null || client.getDocumentPhotoUrl().isBlank()) {
            throw new ConflictException("Complete your profile with a document before requesting a service");
        }
        var category = categories.requireActive(request.categoryId());
        ServiceRequest saved = requests.save(ServiceRequest.builder()
                .client(client)
                .category(category)
                .description(request.description())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .estimatedPrice(request.estimatedPrice())
                .build());
        notifyNearbyTechnicians(saved);
        return map(saved);
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
    public List<ServiceRequestResponse> clientRequests(User user, boolean activeOnly) {
        requireRole(user, Role.CLIENT);
        return (activeOnly
                ? requests.findByClientIdAndStatusInOrderByCreatedAtDesc(user.getId(), CLIENT_ACTIVE_STATUSES)
                : requests.findByClientIdOrderByCreatedAtDesc(user.getId()))
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> clientHistory(User user) {
        requireRole(user, Role.CLIENT);
        return requests.findByClientIdAndStatusInOrderByCreatedAtDesc(user.getId(), HISTORY_STATUSES)
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> assignedRequests(User user, boolean activeOnly) {
        requireRole(user, Role.TECHNICIAN);
        return (activeOnly
                ? requests.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                        user.getId(), TECHNICIAN_ACTIVE_STATUSES)
                : requests.findByTechnicianIdOrderByCreatedAtDesc(user.getId()))
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> assignedHistory(User user) {
        requireRole(user, Role.TECHNICIAN);
        return requests.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(user.getId(), HISTORY_STATUSES)
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse detail(UUID id, User user) {
        ServiceRequest request = find(id);
        boolean participant = request.getClient().getId().equals(user.getId())
                || request.getTechnician() != null
                && request.getTechnician().getId().equals(user.getId());
        if (!participant) throw new ForbiddenException("Only service participants can view this request");
        return map(request);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> available(User technician, double radiusKm) {
        requireRole(technician, Role.TECHNICIAN);
        userAccess.requireActive(technician);
        emailVerification.requireVerified(technician);
        if (radiusKm <= 0 || radiusKm > 100) {
            throw new IllegalArgumentException("radiusKm must be greater than 0 and at most 100");
        }
        var profile = technicianProfiles.approvedProfile(technician);
        if (profile.getLatitude() == null || profile.getLongitude() == null) {
            throw new ConflictException("Technician location is required");
        }
        List<UUID> categoryIds = profile.getCategories().stream().map(item -> item.getId()).toList();
        var liveLocation = technicianLocations.findByTechnicianId(technician.getId()).orElse(null);
        double originLatitude = liveLocation != null && liveLocation.isOnline()
                ? liveLocation.getLatitude() : profile.getLatitude();
        double originLongitude = liveLocation != null && liveLocation.isOnline()
                ? liveLocation.getLongitude() : profile.getLongitude();
        return requests.findAvailable(RequestStatus.QUOTE_PENDING, categoryIds).stream()
                .map(item -> map(item, distance.kilometers(originLatitude, originLongitude,
                        item.getLatitude(), item.getLongitude()), true))
                .filter(item -> item.distanceKm() <= radiusKm)
                .sorted(Comparator.comparing(ServiceRequestResponse::distanceKm))
                .toList();
    }

    @Transactional
    public ServiceQuoteResponse quote(UUID id, BigDecimal technicianPrice, String description, User technician) {
        requireRole(technician, Role.TECHNICIAN);
        requireCriticalAccess(technician);
        emailVerification.requireVerified(technician);
        var profile = technicianProfiles.approvedProfile(technician);
        ServiceRequest request = requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        if (request.getStatus() != RequestStatus.QUOTE_PENDING) {
            throw new ConflictException("Service request is no longer available");
        }
        boolean supportsCategory = profile.getCategories().stream()
                .anyMatch(category -> category.getId().equals(request.getCategory().getId()));
        if (!supportsCategory) throw new ForbiddenException("Technician does not support this category");
        var existing = quotes.findFirstByServiceRequestIdAndTechnicianIdAndStatus(
                id, technician.getId(), QuoteStatus.PENDING);
        if (existing.isPresent()) {
            ServiceQuote pending = existing.get();
            if (pending.getExpiresAt().isAfter(Instant.now())) {
                throw new ConflictException(
                        "You already have a pending quote for this service. Wait for the client response or expiration.");
            }
            pending.setStatus(QuoteStatus.EXPIRED);
            pending.setRespondedAt(Instant.now());
            quotes.saveAndFlush(pending);
        }
        ServiceQuote quote = ServiceQuote.builder()
                .serviceRequest(request)
                .technician(technician)
                .status(QuoteStatus.PENDING)
                .expiresAt(Instant.now().plus(parameters.quoteExpirationMinutes(), ChronoUnit.MINUTES))
                .build();
        if (technicianPrice == null || technicianPrice.signum() <= 0) {
            throw new IllegalArgumentException("Quote price must be greater than zero");
        }
        quote.setPrice(technicianPrice);
        quote.setDescription(clean(description));
        quote = quotes.save(quote);
        events.publishEvent(new UserNotificationEvent(request.getClient().getId(), "Nueva cotización",
                technician.getFullName() + " cotizó tu solicitud por $" + technicianPrice,
                NotificationType.NEW_QUOTE, requestData(request)));
        return mapQuote(quote);
    }

    @Transactional
    public ServiceRequestResponse confirmQuote(UUID id, UUID quoteId, User client) {
        requireRole(client, Role.CLIENT);
        requireCriticalAccess(client);
        ServiceRequest request = requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        requireClientOwner(request, client);
        if (request.getStatus() != RequestStatus.QUOTE_PENDING || request.getTechnician() != null) {
            throw new ConflictException("Service request no longer accepts quotes");
        }
        ServiceQuote selected = quotes.findById(quoteId)
                .orElseThrow(() -> new NotFoundException("Quote not found"));
        if (!selected.getServiceRequest().getId().equals(id) || selected.getStatus() != QuoteStatus.PENDING) {
            throw new ConflictException("Quote is not available for this service request");
        }
        if (!selected.getExpiresAt().isAfter(Instant.now())) {
            selected.setStatus(QuoteStatus.EXPIRED);
            selected.setRespondedAt(Instant.now());
            throw new ConflictException("Quote has expired");
        }
        selected.setStatus(QuoteStatus.ACCEPTED);
        selected.setRespondedAt(Instant.now());
        quotes.findByServiceRequestIdAndStatus(id, QuoteStatus.PENDING).stream()
                .filter(item -> !item.getId().equals(selected.getId()))
                .forEach(item -> {
                    item.setStatus(QuoteStatus.REJECTED);
                    item.setRespondedAt(Instant.now());
                    notifyQuoteRejected(request, item.getTechnician());
                });
        request.setTechnician(selected.getTechnician());
        request.setTechnicianPrice(selected.getPrice());
        request.setFinalPrice(selected.getPrice());
        request.setStatus(RequestStatus.QUOTE_ACCEPTED);
        events.publishEvent(new UserNotificationEvent(selected.getTechnician().getId(), "Cotización aceptada",
                request.getClient().getFullName() + " aceptó tu cotización",
                NotificationType.QUOTE_ACCEPTED, requestData(request)));
        return map(request);
    }

    @Transactional(readOnly = true)
    public List<ServiceQuoteResponse> quotes(UUID id, User client) {
        ServiceRequest request = find(id);
        requireRole(client, Role.CLIENT);
        requireClientOwner(request, client);
        return quotes.findByServiceRequestIdOrderByCreatedAtAsc(id).stream()
                .map(this::mapQuote)
                .toList();
    }

    @Transactional
    public ServiceQuoteResponse rejectQuote(UUID requestId, UUID quoteId, User client) {
        ServiceRequest request = requests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        requireClientOwner(request, client);
        ServiceQuote quote = quotes.findById(quoteId)
                .filter(item -> item.getServiceRequest().getId().equals(requestId))
                .orElseThrow(() -> new NotFoundException("Quote not found"));
        if (quote.getStatus() != QuoteStatus.PENDING) {
            throw new ConflictException("Quote is no longer pending");
        }
        if (!quote.getExpiresAt().isAfter(Instant.now())) {
            quote.setStatus(QuoteStatus.EXPIRED);
            quote.setRespondedAt(Instant.now());
            throw new ConflictException("Quote has expired");
        }
        quote.setStatus(QuoteStatus.REJECTED);
        quote.setRespondedAt(Instant.now());
        notifyQuoteRejected(request, quote.getTechnician());
        return mapQuote(quote);
    }

    @Transactional
    public ServiceRequestResponse updateStatus(UUID id, RequestStatus nextStatus, User user) {
        requireCriticalAccess(user);
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
                statusMessage(nextStatus), notificationType(nextStatus), requestData(request)));
        return map(request);
    }

    private void notifyNearbyTechnicians(ServiceRequest request) {
        technicianProfileRepository.findByStatusOrderByCreatedAtAsc(TechnicianStatus.APPROVED).stream()
                .filter(profile -> profile.getLatitude() != null && profile.getLongitude() != null)
                .filter(profile -> profile.getCategories().stream()
                        .anyMatch(category -> category.getId().equals(request.getCategory().getId())))
                .filter(profile -> distance.kilometers(profile.getLatitude(), profile.getLongitude(),
                        request.getLatitude(), request.getLongitude()) <= newRequestRadiusKm)
                .forEach(profile -> events.publishEvent(new UserNotificationEvent(
                        profile.getUser().getId(),
                        "Nueva solicitud cercana",
                        request.getCategory().getName() + " a menos de " + Math.round(newRequestRadiusKm) + " km",
                        NotificationType.NEW_REQUEST,
                        Map.of(
                                "type", "SERVICE_REQUEST",
                                "requestId", request.getId().toString(),
                                "route", "AvailableRequests"))));
    }

    private NotificationType notificationType(RequestStatus status) {
        return switch (status) {
            case ON_THE_WAY -> NotificationType.TECHNICIAN_ON_THE_WAY;
            case ARRIVED -> NotificationType.TECHNICIAN_ARRIVED;
            case IN_PROGRESS -> NotificationType.SERVICE_STARTED;
            case COMPLETED -> NotificationType.SERVICE_COMPLETED;
            default -> NotificationType.SERVICE_STATUS_CHANGED;
        };
    }

    private String statusMessage(RequestStatus status) {
        return switch (status) {
            case ON_THE_WAY -> "El técnico va en camino";
            case ARRIVED -> "El técnico llegó al lugar del servicio";
            case IN_PROGRESS -> "El servicio ha comenzado";
            case COMPLETED -> "El técnico marcó el servicio como completado";
            default -> "Tu servicio cambió a " + status;
        };
    }

    private Map<String, String> requestData(ServiceRequest request) {
        return Map.of(
                "type", "SERVICE_REQUEST",
                "requestId", request.getId().toString(),
                "route", "RequestDetail");
    }

    private ServiceRequestResponse map(ServiceRequest item) {
        return map(item, null);
    }

    private ServiceRequestResponse map(ServiceRequest item, Double distanceKm) {
        return map(item, distanceKm, false);
    }

    private ServiceRequestResponse map(ServiceRequest item, Double distanceKm, boolean approximateLocation) {
        User technician = item.getTechnician();
        var serviceImages = images.findByServiceRequestIdOrderByCreatedAtAsc(item.getId()).stream()
                .filter(image -> image.getContentAsset() == null
                        || image.getContentAsset().getModerationStatus() == ModerationStatus.APPROVED)
                .toList();
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
                item.getTechnicianPrice(), item.getFinalPrice(), item.getStatus(), item.getCreatedAt(),
                serviceImages.size(),
                serviceImages.isEmpty() ? null : serviceImages.getFirst().getImageUrl(),
                serviceImages.stream().map(image -> new ServiceRequestImageResponse(
                        image.getId(), item.getId(), image.getImageUrl(), image.getPublicId(),
                        image.getContentAsset() == null ? null : image.getContentAsset().getId(),
                        image.getContentAsset() == null ? ModerationStatus.APPROVED
                                : image.getContentAsset().getModerationStatus(),
                        image.getCreatedAt())).toList());
    }

    private ServiceQuoteResponse mapQuote(ServiceQuote quote) {
        User technician = quote.getTechnician();
        return new ServiceQuoteResponse(
                quote.getId(),
                quote.getServiceRequest().getId(),
                technician.getId(),
                technician.getFullName(),
                technician.getProfilePhotoUrl(),
                technician.getAverageRating(),
                technician.getCompletedServicesCount(),
                technician.getWorkExperienceDescription(),
                technicianProfiles.categoryNames(technician),
                quote.getPrice(),
                quote.getDescription(),
                quote.getStatus(),
                quote.getCreatedAt(),
                quote.getUpdatedAt(),
                quote.getExpiresAt(),
                quote.getRespondedAt()
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    private void requireCriticalAccess(User user) {
        userAccess.requireActive(user);
        legal.requireAccepted(user);
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
            events.publishEvent(new UserNotificationEvent(
                    recipient.getId(), title, message, type, requestData(request)));
        }
    }

    private void notifyQuoteRejected(ServiceRequest request, User technician) {
        events.publishEvent(new UserNotificationEvent(
                technician.getId(),
                "Cotización no seleccionada",
                request.getClient().getFullName() + " eligió otra cotización o rechazó tu oferta",
                NotificationType.QUOTE_REJECTED,
                Map.of(
                        "type", "SERVICE_REQUEST",
                        "requestId", request.getId().toString(),
                        "route", "AvailableRequests")));
    }
}
