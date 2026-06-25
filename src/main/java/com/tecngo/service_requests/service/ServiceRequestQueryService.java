package com.tecngo.service_requests.service;

import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.geolocation.HaversineDistance;
import com.tecngo.service_requests.dto.ServiceRequestResponse;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_location.repository.TechnicianLocationRepository;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.UserAccessService;
import com.tecngo.verification.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestQueryService {
    private static final Set<RequestStatus> CLIENT_ACTIVE_STATUSES = Set.of(
            RequestStatus.QUOTE_PENDING, RequestStatus.QUOTED, RequestStatus.QUOTE_ACCEPTED,
            RequestStatus.ON_THE_WAY, RequestStatus.ARRIVED, RequestStatus.IN_PROGRESS,
            RequestStatus.COMPLETED);
    private static final Set<RequestStatus> TECHNICIAN_ACTIVE_STATUSES = Set.of(
            RequestStatus.QUOTE_ACCEPTED, RequestStatus.ON_THE_WAY, RequestStatus.ARRIVED,
            RequestStatus.IN_PROGRESS, RequestStatus.COMPLETED);
    private static final Set<RequestStatus> HISTORY_STATUSES = Set.of(
            RequestStatus.PAID, RequestStatus.CANCELLED, RequestStatus.PAYMENT_DISPUTE);

    private final ServiceRequestRepository requests;
    private final TechnicianProfileService technicianProfiles;
    private final HaversineDistance distance;
    private final EmailVerificationService emailVerification;
    private final SystemParameterService parameters;
    private final TechnicianLocationRepository technicianLocations;
    private final UserAccessService userAccess;
    private final GeographicCatalogService geographicCatalogs;
    private final ServiceRequestAccessPolicy access;
    private final ServiceRequestAssembler assembler;

    @Value("${app.performance.available-request-candidate-limit:500}")
    private int availableRequestCandidateLimit;

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> mine(User user) {
        if (user.isActiveAs(Role.CLIENT)) {
            return clientRequestsPage(user, false, 0, 50).getContent();
        }
        if (user.isActiveAs(Role.TECHNICIAN)) {
            return assignedRequestsPage(user, false, 0, 50).getContent();
        }
        throw new ForbiddenException("An active client or technician mode is required");
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> clientRequests(User user, boolean activeOnly) {
        access.requireRole(user, Role.CLIENT);
        List<ServiceRequest> items = activeOnly
                ? requests.findByClientIdAndStatusInOrderByCreatedAtDesc(user.getId(), CLIENT_ACTIVE_STATUSES)
                : requests.findByClientIdOrderByCreatedAtDesc(user.getId());
        return assembler.responses(items.stream().limit(50).toList(), Map.of(), false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> clientRequestsPage(User user, boolean activeOnly, int page, int size) {
        access.requireRole(user, Role.CLIENT);
        var pageable = pageRequest(page, size);
        Page<ServiceRequest> result = activeOnly
                ? requests.findPageByClientIdAndStatusIn(user.getId(), CLIENT_ACTIVE_STATUSES, pageable)
                : requests.findPageByClientId(user.getId(), pageable);
        return assembler.page(result);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> clientHistory(User user) {
        access.requireRole(user, Role.CLIENT);
        return assembler.responses(requests.findByClientIdAndStatusInOrderByCreatedAtDesc(
                user.getId(), HISTORY_STATUSES).stream().limit(50).toList(), Map.of(), false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> clientHistoryPage(User user, int page, int size) {
        access.requireRole(user, Role.CLIENT);
        return assembler.page(requests.findPageByClientIdAndStatusIn(
                user.getId(), HISTORY_STATUSES, pageRequest(page, size)));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> assignedRequests(User user, boolean activeOnly) {
        access.requireRole(user, Role.TECHNICIAN);
        List<ServiceRequest> items = activeOnly
                ? requests.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                        user.getId(), TECHNICIAN_ACTIVE_STATUSES)
                : requests.findByTechnicianIdOrderByCreatedAtDesc(user.getId());
        return assembler.responses(items.stream().limit(50).toList(), Map.of(), false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> assignedRequestsPage(User user, boolean activeOnly,
                                                              int page, int size) {
        access.requireRole(user, Role.TECHNICIAN);
        var pageable = pageRequest(page, size);
        Page<ServiceRequest> result = activeOnly
                ? requests.findPageByTechnicianIdAndStatusIn(
                        user.getId(), TECHNICIAN_ACTIVE_STATUSES, pageable)
                : requests.findPageByTechnicianId(user.getId(), pageable);
        return assembler.page(result);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> assignedHistory(User user) {
        access.requireRole(user, Role.TECHNICIAN);
        return assembler.responses(requests.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                user.getId(), HISTORY_STATUSES).stream().limit(50).toList(), Map.of(), false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> assignedHistoryPage(User user, int page, int size) {
        access.requireRole(user, Role.TECHNICIAN);
        return assembler.page(requests.findPageByTechnicianIdAndStatusIn(
                user.getId(), HISTORY_STATUSES, pageRequest(page, size)));
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse detail(UUID id, User user) {
        ServiceRequest request = find(id);
        access.requireParticipant(request, user);
        return assembler.response(request);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> available(User technician, UUID requestedCityId,
                                                  UUID requestedCategoryId, Boolean requestedUseRadius,
                                                  Double requestedRadiusKm) {
        access.requireRole(technician, Role.TECHNICIAN);
        userAccess.requireActive(technician);
        emailVerification.requireVerified(technician);
        var profile = technicianProfiles.approvedProfile(technician);
        List<UUID> categoryIds = profile.getCategories().stream().map(item -> item.getId()).toList();
        if (categoryIds.isEmpty()) throw new ConflictException("Technician categories are required");
        if (requestedCategoryId != null && !categoryIds.contains(requestedCategoryId)) {
            throw new ForbiddenException("Technician does not support the selected category");
        }
        var searchCity = requestedCityId != null
                ? geographicCatalogs.requireCity(requestedCityId)
                : technician.getCity();
        boolean useRadius = requestedUseRadius != null
                ? requestedUseRadius
                : requestedRadiusKm != null || parameters.serviceSearchUseRadius();
        Double radiusKm = resolveRadius(requestedRadiusKm, useRadius);
        var liveLocation = technicianLocations.findByTechnicianId(technician.getId()).orElse(null);
        Double originLatitude;
        Double originLongitude;
        if (liveLocation != null && liveLocation.isOnline()) {
            originLatitude = liveLocation.getLatitude();
            originLongitude = liveLocation.getLongitude();
        } else {
            originLatitude = profile.getLatitude();
            originLongitude = profile.getLongitude();
        }
        boolean hasOriginLocation = originLatitude != null && originLongitude != null;
        if (useRadius && !hasOriginLocation) {
            throw new ConflictException("Technician GPS location is required");
        }
        var candidates = PageRequest.of(0,
                availableRequestCandidateLimit > 0 ? Math.min(availableRequestCandidateLimit, 2000) : 500);
        List<ServiceRequest> availableRequests = searchCity == null
                ? requests.findAvailableWithoutCity(
                        RequestStatus.QUOTE_PENDING, categoryIds, requestedCategoryId, candidates)
                : requests.findAvailable(RequestStatus.QUOTE_PENDING, searchCity.getId(),
                        categoryIds, requestedCategoryId, candidates);
        Map<UUID, Double> distances = hasOriginLocation
                ? distances(availableRequests, originLatitude, originLongitude)
                : Map.of();
        Double activeRadiusKm = radiusKm;
        List<ServiceRequest> sorted = availableRequests.stream()
                .filter(item -> !item.getClient().getId().equals(technician.getId()))
                .filter(item -> !useRadius || distances.containsKey(item.getId())
                        && distances.get(item.getId()) <= activeRadiusKm)
                .sorted(Comparator
                        .comparing((ServiceRequest item) -> distances.get(item.getId()),
                                Comparator.nullsLast(Double::compareTo))
                        .thenComparing(ServiceRequest::getCreatedAt, Comparator.reverseOrder()))
                .toList();
        return assembler.responses(sorted, distances, true, technician);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> availablePage(
            User technician, UUID requestedCityId, UUID requestedCategoryId,
            Boolean requestedUseRadius, Double requestedRadiusKm, int page, int size) {
        List<ServiceRequestResponse> items = available(technician, requestedCityId,
                requestedCategoryId, requestedUseRadius, requestedRadiusKm);
        int boundedSize = Math.max(1, Math.min(size, 100));
        int boundedPage = Math.max(0, page);
        int from = Math.min(boundedPage * boundedSize, items.size());
        int to = Math.min(from + boundedSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(boundedPage, boundedSize),
                items.size());
    }

    private Double resolveRadius(Double requestedRadiusKm, boolean useRadius) {
        if (!useRadius) return null;
        double maxRadiusKm = parameters.serviceSearchMaxRadiusKm().doubleValue();
        double radiusKm = requestedRadiusKm == null
                ? parameters.serviceSearchDefaultRadiusKm().doubleValue()
                : requestedRadiusKm;
        if (radiusKm <= 0 || radiusKm > maxRadiusKm) {
            throw new IllegalArgumentException(
                    "radiusKm must be greater than 0 and at most " + maxRadiusKm);
        }
        return radiusKm;
    }

    private Map<UUID, Double> distances(List<ServiceRequest> items,
                                        Double originLatitude, Double originLongitude) {
        Map<UUID, Double> values = new HashMap<>();
        items.forEach(item -> {
            Double value = calculateDistance(originLatitude, originLongitude,
                    item.getLatitude(), item.getLongitude());
            if (value != null) values.put(item.getId(), value);
        });
        return values;
    }

    private Double calculateDistance(Double originLatitude, Double originLongitude,
                                     Double destinationLatitude, Double destinationLongitude) {
        if (originLatitude == null || originLongitude == null
                || destinationLatitude == null || destinationLongitude == null) return null;
        return distance.kilometers(originLatitude, originLongitude,
                destinationLatitude, destinationLongitude);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private ServiceRequest find(UUID id) {
        return requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
    }
}
