package com.tecngo.service_requests.service;

import com.tecngo.catalogs.service.GeographicCatalogService;
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
import com.tecngo.geolocation.LocationPrecision;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.payments.entity.Payment;
import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.payments.entity.PaymentStatus;
import com.tecngo.payments.repository.PaymentRepository;
import com.tecngo.payments.service.PlatformFeeCalculator;
import com.tecngo.referrals.entity.ReferralReward;
import com.tecngo.referrals.service.ReferralService;
import com.tecngo.reports.entity.ReportReason;
import com.tecngo.reports.entity.ReportSeverity;
import com.tecngo.reports.entity.UserReport;
import com.tecngo.reports.repository.UserReportRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import com.tecngo.technician_location.repository.TechnicianLocationRepository;
import com.tecngo.content_moderation.entity.ModerationStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

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
            RequestStatus.PAID, RequestStatus.CANCELLED, RequestStatus.PAYMENT_DISPUTE);
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
    private final GeographicCatalogService geographicCatalogs;
    private final PaymentRepository payments;
    private final PlatformFeeCalculator feeCalculator;
    private final ReferralService referrals;
    private final UserReportRepository reports;
    private final TechnicianWalletService wallets;
    @Value("${app.notifications.new-request-radius-km:25}")
    private double newRequestRadiusKm;
    @Value("${app.performance.available-request-candidate-limit:500}")
    private int availableRequestCandidateLimit;

    @Transactional
    public ServiceRequestResponse create(CreateServiceRequest request, User client) {
        requireRole(client, Role.CLIENT);
        requireCriticalAccess(client);
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
        notifyNearbyTechnicians(saved);
        return map(saved);
    }

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
        requireRole(user, Role.CLIENT);
        List<ServiceRequest> items = activeOnly
                ? requests.findByClientIdAndStatusInOrderByCreatedAtDesc(user.getId(), CLIENT_ACTIVE_STATUSES)
                : requests.findByClientIdOrderByCreatedAtDesc(user.getId());
        return mapAll(items.stream().limit(50).toList(), Map.of(), false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> clientRequestsPage(User user, boolean activeOnly, int page, int size) {
        requireRole(user, Role.CLIENT);
        var pageable = pageRequest(page, size);
        Page<ServiceRequest> result = activeOnly
                ? requests.findPageByClientIdAndStatusIn(user.getId(), CLIENT_ACTIVE_STATUSES, pageable)
                : requests.findPageByClientId(user.getId(), pageable);
        return mapPage(result);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> clientHistory(User user) {
        requireRole(user, Role.CLIENT);
        return mapAll(requests.findByClientIdAndStatusInOrderByCreatedAtDesc(
                user.getId(), HISTORY_STATUSES).stream().limit(50).toList(), Map.of(), false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> clientHistoryPage(User user, int page, int size) {
        requireRole(user, Role.CLIENT);
        return mapPage(requests.findPageByClientIdAndStatusIn(
                user.getId(), HISTORY_STATUSES, pageRequest(page, size)));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> assignedRequests(User user, boolean activeOnly) {
        requireRole(user, Role.TECHNICIAN);
        List<ServiceRequest> items = activeOnly
                ? requests.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                        user.getId(), TECHNICIAN_ACTIVE_STATUSES)
                : requests.findByTechnicianIdOrderByCreatedAtDesc(user.getId());
        return mapAll(items.stream().limit(50).toList(), Map.of(), false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> assignedRequestsPage(User user, boolean activeOnly,
                                                              int page, int size) {
        requireRole(user, Role.TECHNICIAN);
        var pageable = pageRequest(page, size);
        Page<ServiceRequest> result = activeOnly
                ? requests.findPageByTechnicianIdAndStatusIn(
                        user.getId(), TECHNICIAN_ACTIVE_STATUSES, pageable)
                : requests.findPageByTechnicianId(user.getId(), pageable);
        return mapPage(result);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> assignedHistory(User user) {
        requireRole(user, Role.TECHNICIAN);
        return mapAll(requests.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                user.getId(), HISTORY_STATUSES).stream().limit(50).toList(), Map.of(), false);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> assignedHistoryPage(User user, int page, int size) {
        requireRole(user, Role.TECHNICIAN);
        return mapPage(requests.findPageByTechnicianIdAndStatusIn(
                user.getId(), HISTORY_STATUSES, pageRequest(page, size)));
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
    public List<ServiceRequestResponse> available(User technician, UUID requestedCityId,
                                                  UUID requestedCategoryId, Boolean requestedUseRadius,
                                                  Double requestedRadiusKm) {
        requireRole(technician, Role.TECHNICIAN);
        userAccess.requireActive(technician);
        emailVerification.requireVerified(technician);
        var profile = technicianProfiles.approvedProfile(technician);
        List<UUID> categoryIds = profile.getCategories().stream().map(item -> item.getId()).toList();
        if (categoryIds.isEmpty()) {
            throw new ConflictException("Technician categories are required");
        }
        if (requestedCategoryId != null && !categoryIds.contains(requestedCategoryId)) {
            throw new ForbiddenException("Technician does not support the selected category");
        }
        var searchCity = requestedCityId != null
                ? geographicCatalogs.requireCity(requestedCityId)
                : technician.getCity();

        boolean useRadius = requestedUseRadius != null
                ? requestedUseRadius
                : requestedRadiusKm != null || parameters.serviceSearchUseRadius();
        Double radiusKm = null;
        if (useRadius) {
            double maxRadiusKm = parameters.serviceSearchMaxRadiusKm().doubleValue();
            radiusKm = requestedRadiusKm == null
                    ? parameters.serviceSearchDefaultRadiusKm().doubleValue()
                    : requestedRadiusKm;
            if (radiusKm <= 0 || radiusKm > maxRadiusKm) {
                throw new IllegalArgumentException(
                        "radiusKm must be greater than 0 and at most " + maxRadiusKm);
            }
        }

        var liveLocation = technicianLocations.findByTechnicianId(technician.getId()).orElse(null);
        Double originLatitude = liveLocation != null && liveLocation.isOnline()
                ? liveLocation.getLatitude() : profile.getLatitude();
        Double originLongitude = liveLocation != null && liveLocation.isOnline()
                ? liveLocation.getLongitude() : profile.getLongitude();
        if (useRadius && (originLatitude == null || originLongitude == null)) {
            throw new ConflictException("Technician GPS location is required");
        }

        Double activeRadiusKm = radiusKm;
        var candidates = PageRequest.of(0,
                availableRequestCandidateLimit > 0 ? Math.min(availableRequestCandidateLimit, 2000) : 500);
        List<ServiceRequest> availableRequests = searchCity == null
                ? requests.findAvailableWithoutCity(
                        RequestStatus.QUOTE_PENDING, categoryIds, requestedCategoryId, candidates)
                : requests.findAvailable(RequestStatus.QUOTE_PENDING, searchCity.getId(),
                        categoryIds, requestedCategoryId, candidates);
        Map<UUID, Double> distances = new HashMap<>();
        availableRequests.forEach(item -> {
            Double value = calculateDistance(originLatitude, originLongitude,
                    item.getLatitude(), item.getLongitude());
            if (value != null) distances.put(item.getId(), value);
        });
        List<ServiceRequest> sorted = availableRequests.stream()
                .filter(item -> !item.getClient().getId().equals(technician.getId()))
                .filter(item -> !useRadius || distances.containsKey(item.getId())
                        && distances.get(item.getId()) <= activeRadiusKm)
                .sorted(Comparator
                        .comparing((ServiceRequest item) -> distances.get(item.getId()),
                                Comparator.nullsLast(Double::compareTo))
                        .thenComparing(ServiceRequest::getCreatedAt, Comparator.reverseOrder()))
                .toList();
        return mapAll(sorted, distances, true);
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

    @Transactional
    public ServiceQuoteResponse quote(UUID id, BigDecimal technicianPrice, String description, User technician) {
        requireRole(technician, Role.TECHNICIAN);
        requireCriticalAccess(technician);
        emailVerification.requireVerified(technician);
        ServiceRequest request = requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        requireDifferentUser(request.getClient(), technician,
                "You cannot quote your own service request");
        if (request.getStatus() != RequestStatus.QUOTE_PENDING) {
            throw new ConflictException("Service request is no longer available");
        }
        var profile = technicianProfiles.approvedProfile(technician);
        wallets.requireCanQuote(technician);
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
        events.publishEvent(new UserNotificationEvent(request.getClient().getId(), "Nueva cotización recibida",
                technician.getFullName() + " cotizó " + formatCop(technicianPrice)
                        + " para tu solicitud en" + request.getCategory().getName(),
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
        requireDifferentUser(client, selected.getTechnician(),
                "You cannot accept a quote created by your own account");
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
        events.publishEvent(new UserNotificationEvent(selected.getTechnician().getId(),
                "Cotización aceptada por el cliente",
                request.getClient().getFullName() + " aceptó tu cotización para "
                        + request.getCategory().getName(),
                NotificationType.QUOTE_ACCEPTED, requestData(request)));
        return map(request);
    }

    @Transactional(readOnly = true)
    public List<ServiceQuoteResponse> quotes(UUID id, User client) {
        ServiceRequest request = find(id);
        requireRole(client, Role.CLIENT);
        requireClientOwner(request, client);
        List<ServiceQuote> items = quotes.findByServiceRequestIdOrderByCreatedAtAsc(id);
        Map<UUID, List<String>> categoryMap = loadTechnicianCategories(items.stream()
                .map(item -> item.getTechnician().getId())
                .distinct()
                .toList());
        return items.stream()
                .map(item -> mapQuote(item,
                        categoryMap.getOrDefault(item.getTechnician().getId(), List.of())))
                .toList();
    }

    @Transactional
    public ServiceQuoteResponse rejectQuote(UUID requestId, UUID quoteId, User client) {
        requireRole(client, Role.CLIENT);
        requireCriticalAccess(client);
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
        if (nextStatus == RequestStatus.CANCELLED && user.isActiveAs(Role.CLIENT)) {
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
        events.publishEvent(new UserNotificationEvent(request.getClient().getId(), statusTitle(nextStatus),
                statusMessage(nextStatus), notificationType(nextStatus), requestData(request)));
        return map(request);
    }

    @Transactional
    public ServiceRequestResponse technicianComplete(UUID id, boolean paymentReceived,
                                                     PaymentMethod paymentMethod, String comment,
                                                     User technician) {
        requireRole(technician, Role.TECHNICIAN);
        requireCriticalAccess(technician);
        ServiceRequest request = requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        requireAssignedTechnician(request, technician);
        if (request.getStatus() == RequestStatus.PAID || request.getStatus() == RequestStatus.PAYMENT_DISPUTE
                || request.getStatus() == RequestStatus.CANCELLED) {
            throw new ConflictException("Service request is already closed");
        }
        if (!Set.of(RequestStatus.QUOTE_ACCEPTED, RequestStatus.ON_THE_WAY, RequestStatus.ARRIVED,
                RequestStatus.IN_PROGRESS, RequestStatus.COMPLETED).contains(request.getStatus())) {
            throw new ConflictException("Service request cannot be completed from current status");
        }
        if (request.getFinalPrice() == null || request.getFinalPrice().signum() <= 0) {
            throw new ConflictException("Service request does not have a final price");
        }
        if (paymentReceived) {
            createPaidPayment(request, paymentMethod == null ? request.getRequestedPaymentMethod() : paymentMethod);
            request.setStatus(RequestStatus.PAID);
            request.getClient().setCompletedServicesCount(request.getClient().getCompletedServicesCount() + 1);
            request.getTechnician().setCompletedServicesCount(request.getTechnician().getCompletedServicesCount() + 1);
            request.getClient().setPaidServicesCount(request.getClient().getPaidServicesCount() + 1);
            request.getTechnician().setPaidServicesCount(request.getTechnician().getPaidServicesCount() + 1);
            events.publishEvent(new UserNotificationEvent(request.getClient().getId(), "Servicio pagado",
                    "El técnico confirmó el pago. Puedes calificar el servicio.",
                    NotificationType.SERVICE_COMPLETED, requestData(request)));
        } else {
            request.setStatus(RequestStatus.PAYMENT_DISPUTE);
            reports.save(UserReport.builder()
                    .serviceRequest(request)
                    .reporter(technician)
                    .reported(request.getClient())
                    .reporterRole(technician.getRole())
                    .reportedRole(request.getClient().getRole())
                    .reason(ReportReason.PAYMENT_ISSUE)
                    .description(clean(comment) == null
                            ? "El técnico reportó que el cliente no pagó el valor acordado."
                            : clean(comment))
                    .severity(ReportSeverity.HIGH)
                    .build());
            events.publishEvent(new UserNotificationEvent(request.getClient().getId(), "Pago reportado",
                    "El técnico reportó un problema de pago en el servicio.",
                    NotificationType.SERVICE_STATUS_CHANGED, requestData(request)));
        }
        return map(request);
    }

    private void createPaidPayment(ServiceRequest request, PaymentMethod method) {
        if (payments.existsByServiceRequestId(request.getId())) {
            throw new ConflictException("Service request is already paid");
        }
        BigDecimal amount = request.getFinalPrice();
        BigDecimal percentage = parameters.platformCommissionPercentage();
        ReferralReward reward = referrals.useAvailableReward(request.getTechnician(), request, percentage);
        boolean commissionWaived = reward != null;
        BigDecimal effectivePercentage = commissionWaived ? BigDecimal.ZERO : percentage;
        Payment payment = payments.save(Payment.builder()
                .serviceRequest(request)
                .client(request.getClient())
                .technician(request.getTechnician())
                .amount(amount)
                .platformFee(feeCalculator.fee(amount, effectivePercentage))
                .platformCommissionPercentage(effectivePercentage)
                .technicianAmount(feeCalculator.technicianAmount(amount, effectivePercentage))
                .commissionWaived(commissionWaived)
                .commissionWaivedReason(commissionWaived ? "REFERRAL_REWARD" : null)
                .referralReward(reward)
                .status(PaymentStatus.PAID)
                .method(method == null ? PaymentMethod.CASH : method)
                .build());
        payment.setTechnicianWalletTransactionId(wallets.debitCommissionIfEnabled(payment));
    }

    private void notifyNearbyTechnicians(ServiceRequest request) {
        technicianProfileRepository.findByStatusOrderByCreatedAtAsc(TechnicianStatus.APPROVED).stream()
                .filter(com.tecngo.technicians.entity.TechnicianProfile::isAvailable)
                .filter(profile -> profile.getLatitude() != null && profile.getLongitude() != null)
                .filter(profile -> profile.getCategories().stream()
                        .anyMatch(category -> category.getId().equals(request.getCategory().getId())))
                .filter(profile -> profile.getUser().getCity() == null || request.getCity() == null
                        || profile.getUser().getCity().getId().equals(request.getCity().getId()))
                .filter(profile -> distance.kilometers(profile.getLatitude(), profile.getLongitude(),
                        request.getLatitude(), request.getLongitude()) <= newRequestRadiusKm)
                .forEach(profile -> events.publishEvent(new UserNotificationEvent(
                        profile.getUser().getId(),
                        "Nueva solicitud cercana disponible",
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

    private String statusTitle(RequestStatus status) {
        return switch (status) {
            case ON_THE_WAY -> "Técnico en camino";
            case ARRIVED -> "Técnico llegó al servicio";
            case IN_PROGRESS -> "Servicio iniciado";
            case COMPLETED -> "Servicio finalizado";
            default -> "Estado del servicio actualizado";
        };
    }

    private String formatCop(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"));
        return "$" + formatter.format(value.setScale(0, java.math.RoundingMode.HALF_UP)) + " COP";
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
        Map<UUID, List<com.tecngo.service_requests.entity.ServiceRequestImage>> imageMap =
                loadImages(List.of(item.getId()));
        Map<UUID, List<String>> categoryMap = loadTechnicianCategories(
                item.getTechnician() == null ? List.of() : List.of(item.getTechnician().getId()));
        return map(item, distanceKm, approximateLocation,
                imageMap.getOrDefault(item.getId(), List.of()),
                item.getTechnician() == null
                        ? List.of()
                        : categoryMap.getOrDefault(item.getTechnician().getId(), List.of()));
    }

    private ServiceRequestResponse map(
            ServiceRequest item,
            Double distanceKm,
            boolean approximateLocation,
            List<com.tecngo.service_requests.entity.ServiceRequestImage> serviceImages,
            List<String> technicianCategories
    ) {
        User technician = item.getTechnician();
        return new ServiceRequestResponse(item.getId(), item.getClient().getId(), item.getClient().getFullName(),
                technician == null ? null : technician.getId(), technician == null ? null : technician.getFullName(),
                item.getClient().getProfilePhotoUrl(), item.getClient().getAverageRating(),
                item.getClient().getPaidServicesCount(),
                technician == null ? null : technician.getProfilePhotoUrl(),
                technician == null ? null : technician.getAverageRating(),
                technician == null ? 0 : technician.getCompletedServicesCount(),
                technician == null ? null : technician.getWorkExperienceDescription(),
                technicianCategories,
                technician != null && technician.isDocumentsVerified()
                        && !blank(technician.getCertificatePhotoUrl()),
                item.getCategory().getId(), item.getCategory().getName(), item.getDescription(),
                approximateLocation ? approximate(item.getAddress()) : item.getAddress(),
                approximateLocation ? approximateCoordinate(item.getLatitude()) : item.getLatitude(),
                approximateLocation ? approximateCoordinate(item.getLongitude()) : item.getLongitude(),
                approximateLocation ? LocationPrecision.APPROXIMATE : LocationPrecision.EXACT,
                distanceKm, item.getEstimatedPrice(),
                item.getTechnicianPrice(), item.getFinalPrice(), item.getRequestedPaymentMethod(),
                item.getStatus(), item.getCreatedAt(),
                serviceImages.size(),
                serviceImages.isEmpty() ? null : serviceImages.getFirst().getImageUrl(),
                serviceImages.stream().map(image -> new ServiceRequestImageResponse(
                        image.getId(), item.getId(), image.getImageUrl(), image.getPublicId(),
                        image.getContentAsset() == null ? null : image.getContentAsset().getId(),
                        image.getContentAsset() == null ? ModerationStatus.APPROVED
                                : image.getContentAsset().getModerationStatus(),
                        image.getCreatedAt())).toList(),
                item.getCity() == null ? null : item.getCity().getId(),
                item.getCity() == null ? null : item.getCity().getName());
    }

    private Page<ServiceRequestResponse> mapPage(Page<ServiceRequest> page) {
        return new PageImpl<>(mapAll(page.getContent(), Map.of(), false),
                page.getPageable(), page.getTotalElements());
    }

    private List<ServiceRequestResponse> mapAll(List<ServiceRequest> items,
                                                Map<UUID, Double> distances,
                                                boolean approximateLocation) {
        if (items.isEmpty()) return List.of();
        Map<UUID, List<com.tecngo.service_requests.entity.ServiceRequestImage>> imageMap =
                loadImages(items.stream().map(ServiceRequest::getId).toList());
        Map<UUID, List<String>> categoryMap = loadTechnicianCategories(items.stream()
                .map(ServiceRequest::getTechnician)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList());
        return items.stream().map(item -> {
            UUID technicianId = item.getTechnician() == null ? null : item.getTechnician().getId();
            return map(item, distances.get(item.getId()), approximateLocation,
                    imageMap.getOrDefault(item.getId(), List.of()),
                    technicianId == null ? List.of() : categoryMap.getOrDefault(technicianId, List.of()));
        }).toList();
    }

    private Map<UUID, List<com.tecngo.service_requests.entity.ServiceRequestImage>> loadImages(
            List<UUID> requestIds) {
        if (requestIds.isEmpty()) return Map.of();
        return images.findByServiceRequestIdInOrderByCreatedAtAsc(requestIds).stream()
                .filter(image -> image.getContentAsset() == null
                        || image.getContentAsset().getModerationStatus() == ModerationStatus.APPROVED)
                .collect(Collectors.groupingBy(image -> image.getServiceRequest().getId()));
    }

    private Map<UUID, List<String>> loadTechnicianCategories(Collection<UUID> technicianIds) {
        if (technicianIds.isEmpty()) return Map.of();
        return technicianProfileRepository.findWithCategoriesByUserIdIn(technicianIds).stream()
                .collect(Collectors.toMap(
                        profile -> profile.getUser().getId(),
                        profile -> profile.getCategories().stream()
                                .map(category -> category.getName())
                                .sorted()
                                .toList()));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private ServiceQuoteResponse mapQuote(ServiceQuote quote) {
        List<String> categories = loadTechnicianCategories(
                List.of(quote.getTechnician().getId()))
                .getOrDefault(quote.getTechnician().getId(), List.of());
        return mapQuote(quote, categories);
    }

    private ServiceQuoteResponse mapQuote(ServiceQuote quote, List<String> technicianCategories) {
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
                technicianCategories,
                technician.isDocumentsVerified() && !blank(technician.getCertificatePhotoUrl()),
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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String approximate(String address) {
        String[] parts = address.split(",");
        if (parts.length < 2) return "Zona cercana";
        int start = Math.max(1, parts.length - 2);
        return String.join(", ", java.util.Arrays.stream(parts, start, parts.length)
                .map(String::trim).toList());
    }

    private Double approximateCoordinate(Double coordinate) {
        if (coordinate == null) return null;
        return Math.round(coordinate * 100.0) / 100.0;
    }

    private Double calculateDistance(Double originLatitude, Double originLongitude,
                                     Double destinationLatitude, Double destinationLongitude) {
        if (originLatitude == null || originLongitude == null
                || destinationLatitude == null || destinationLongitude == null) {
            return null;
        }
        return distance.kilometers(originLatitude, originLongitude,
                destinationLatitude, destinationLongitude);
    }

    private ServiceRequest find(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Service request not found"));
    }

    private void requireRole(User user, Role role) {
        if (!user.hasRole(role)) {
            throw new ForbiddenException("Role " + role + " is required");
        }
        if ((role == Role.CLIENT || role == Role.TECHNICIAN) && !user.isActiveAs(role)) {
            throw new ForbiddenException("Active mode " + role + " is required");
        }
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

    private void requireDifferentUser(User first, User second, String message) {
        if (first.getId().equals(second.getId())) {
            throw new ForbiddenException(message);
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
                request.getClient().getFullName() + " eligió otra cotización o rechazó tu oferta para "
                        + request.getCategory().getName(),
                NotificationType.QUOTE_REJECTED,
                Map.of(
                        "type", "SERVICE_REQUEST",
                        "requestId", request.getId().toString(),
                        "route", "AvailableRequests")));
    }
}
