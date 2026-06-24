package com.tecngo.service_requests.service;

import com.tecngo.catalogs.entity.City;
import com.tecngo.catalogs.service.GeographicCatalogService;
import com.tecngo.geolocation.HaversineDistance;
import com.tecngo.content_moderation.entity.ContentAsset;
import com.tecngo.content_moderation.entity.ModerationStatus;
import com.tecngo.legal.service.LegalService;
import com.tecngo.notifications.entity.NotificationType;
import com.tecngo.notifications.event.UserNotificationEvent;
import com.tecngo.payments.entity.Payment;
import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.payments.repository.PaymentRepository;
import com.tecngo.payments.service.PlatformFeeCalculator;
import com.tecngo.referrals.service.ReferralService;
import com.tecngo.reports.entity.ReportReason;
import com.tecngo.reports.entity.UserReport;
import com.tecngo.reports.repository.UserReportRepository;
import com.tecngo.service_requests.dto.CreateServiceRequest;
import com.tecngo.service_requests.entity.QuoteStatus;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceQuote;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.entity.ServiceRequestImage;
import com.tecngo.service_requests.repository.ServiceQuoteRepository;
import com.tecngo.service_requests.repository.ServiceRequestImageRepository;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.services.service.ServiceCategoryService;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.technician_location.repository.TechnicianLocationRepository;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.service.UserAccessService;
import com.tecngo.verification.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceCharacterizationTest {
    @Mock ServiceRequestRepository requests;
    @Mock ServiceQuoteRepository quotes;
    @Mock ServiceRequestImageRepository images;
    @Mock ServiceCategoryService categories;
    @Mock TechnicianProfileService technicianProfiles;
    @Mock TechnicianProfileRepository technicianProfileRepository;
    @Mock HaversineDistance distance;
    @Mock ApplicationEventPublisher events;
    @Mock EmailVerificationService emailVerification;
    @Mock SystemParameterService parameters;
    @Mock TechnicianLocationRepository technicianLocations;
    @Mock UserAccessService userAccess;
    @Mock LegalService legal;
    @Mock GeographicCatalogService geographicCatalogs;
    @Mock PaymentRepository payments;
    @Mock PlatformFeeCalculator feeCalculator;
    @Mock ReferralService referrals;
    @Mock UserReportRepository reports;
    @Mock TechnicianWalletService wallets;
    ServiceRequestService service;

    private ServiceCategory category;
    private City city;
    private User client;
    private User technician;

    @BeforeEach
    void setUp() {
        ServiceRequestAccessPolicy access = new ServiceRequestAccessPolicy(userAccess, legal);
        ServiceRequestAssembler assembler = new ServiceRequestAssembler(images, technicianProfileRepository, quotes);
        ServiceRequestNotifier notifier = new ServiceRequestNotifier(events, technicianProfileRepository, distance);
        ServiceRequestCommandService commands = new ServiceRequestCommandService(
                requests, categories, geographicCatalogs, emailVerification, access, notifier, assembler,
                parameters);
        ServiceRequestQueryService queries = new ServiceRequestQueryService(
                requests, technicianProfiles, distance, emailVerification, parameters, technicianLocations,
                userAccess, geographicCatalogs, access, assembler);
        ServiceQuoteService quoteService = new ServiceQuoteService(
                requests, quotes, technicianProfiles, emailVerification, parameters, wallets,
                access, assembler, notifier);
        ServiceLifecycleService lifecycle = new ServiceLifecycleService(
                requests, payments, feeCalculator, referrals, reports, wallets, parameters,
                access, assembler, notifier);
        service = new ServiceRequestService(commands, queries, quoteService, lifecycle);

        category = ServiceCategory.builder().id(UUID.randomUUID()).name("Electricista").active(true).build();
        city = City.builder().id(UUID.randomUUID()).name("Villavicencio").build();
        client = user(Role.CLIENT, "Cliente");
        client.setDocumentPhotoUrl("/private/document");
        client.setCity(city);
        technician = user(Role.TECHNICIAN, "Técnico");
        ReflectionTestUtils.setField(notifier, "newRequestRadiusKm", 25d);
        ReflectionTestUtils.setField(queries, "availableRequestCandidateLimit", 500);
    }

    @Test
    void createPersistsProfileCityDefaultPaymentMethodAndPublishesNearbyRequest() {
        TechnicianProfile nearby = TechnicianProfile.builder()
                .user(technician).status(com.tecngo.technicians.entity.TechnicianStatus.APPROVED)
                .available(true).latitude(4.14).longitude(-73.63).categories(Set.of(category)).build();
        when(categories.requireActive(category.getId())).thenReturn(category);
        when(requests.save(any(ServiceRequest.class))).thenAnswer(invocation -> {
            ServiceRequest saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setStatus(RequestStatus.QUOTE_PENDING);
            saved.setCreatedAt(Instant.now());
            return saved;
        });
        when(technicianProfileRepository.findByStatusOrderByCreatedAtAsc(
                com.tecngo.technicians.entity.TechnicianStatus.APPROVED)).thenReturn(List.of(nearby));
        when(distance.kilometers(any(Double.class), any(Double.class), any(Double.class), any(Double.class)))
                .thenReturn(3d);
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());

        var response = service.create(new CreateServiceRequest(category.getId(), "Instalación",
                "Calle 10", 4.15, -73.64, new BigDecimal("100000"), null, null), client);

        ArgumentCaptor<ServiceRequest> saved = ArgumentCaptor.forClass(ServiceRequest.class);
        verify(requests).save(saved.capture());
        assertThat(saved.getValue().getCity()).isEqualTo(city);
        assertThat(saved.getValue().getRequestedPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.status()).isEqualTo(RequestStatus.QUOTE_PENDING);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(UserNotificationEvent.class);
        assertThat(((UserNotificationEvent) event.getValue()).type()).isEqualTo(NotificationType.NEW_REQUEST);
    }

    @Test
    void createRequiresClientDocumentBeforeAnyPersistence() {
        client.setDocumentPhotoUrl(null);

        assertThatThrownBy(() -> service.create(new CreateServiceRequest(category.getId(), "Instalación",
                "Calle 10", 4.15, -73.64, null, null, PaymentMethod.CASH), client))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Complete your profile with a document");
        verify(requests, never()).save(any());
    }

    @Test
    void quotePersistsPriceDescriptionAndNotifiesClient() {
        ServiceRequest request = request(RequestStatus.QUOTE_PENDING);
        TechnicianProfile profile = TechnicianProfile.builder()
                .user(technician).categories(Set.of(category)).build();
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(technicianProfiles.approvedProfile(technician)).thenReturn(profile);
        when(quotes.findFirstByServiceRequestIdAndTechnicianIdAndStatus(
                request.getId(), technician.getId(), QuoteStatus.PENDING)).thenReturn(Optional.empty());
        when(parameters.quoteExpirationMinutes()).thenReturn(30);
        when(quotes.save(any(ServiceQuote.class))).thenAnswer(invocation -> {
            ServiceQuote saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(saved.getCreatedAt());
            return saved;
        });
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of(profile));

        var response = service.quote(request.getId(), new BigDecimal("120000"), "  Incluye visita  ", technician);

        assertThat(response.price()).isEqualByComparingTo("120000");
        assertThat(response.description()).isEqualTo("Incluye visita");
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        UserNotificationEvent notification = (UserNotificationEvent) event.getValue();
        assertThat(notification.type()).isEqualTo(NotificationType.NEW_QUOTE);
        assertThat(notification.message()).contains("$120.000 COP", "Electricista");
    }

    @Test
    void quoteRejectsASecondUnexpiredPendingOffer() {
        ServiceRequest request = request(RequestStatus.QUOTE_PENDING);
        TechnicianProfile profile = TechnicianProfile.builder().user(technician)
                .categories(Set.of(category)).build();
        ServiceQuote pending = quote(request, technician, QuoteStatus.PENDING);
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(technicianProfiles.approvedProfile(technician)).thenReturn(profile);
        when(quotes.findFirstByServiceRequestIdAndTechnicianIdAndStatus(
                request.getId(), technician.getId(), QuoteStatus.PENDING)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.quote(request.getId(), new BigDecimal("120000"), null, technician))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already have a pending quote");
        verify(quotes, never()).save(any());
    }

    @Test
    void quoteExpiresOldPendingOfferAndAcceptsReplacement() {
        ServiceRequest request = request(RequestStatus.QUOTE_PENDING);
        TechnicianProfile profile = TechnicianProfile.builder().user(technician)
                .categories(Set.of(category)).build();
        ServiceQuote pending = quote(request, technician, QuoteStatus.PENDING);
        pending.setExpiresAt(Instant.now().minusSeconds(1));
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(technicianProfiles.approvedProfile(technician)).thenReturn(profile);
        when(quotes.findFirstByServiceRequestIdAndTechnicianIdAndStatus(
                request.getId(), technician.getId(), QuoteStatus.PENDING)).thenReturn(Optional.of(pending));
        when(parameters.quoteExpirationMinutes()).thenReturn(30);
        when(quotes.save(any(ServiceQuote.class))).thenAnswer(invocation -> {
            ServiceQuote saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(saved.getCreatedAt());
            return saved;
        });
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of(profile));

        service.quote(request.getId(), new BigDecimal("125000"), "Nueva oferta", technician);

        assertThat(pending.getStatus()).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(pending.getRespondedAt()).isNotNull();
        verify(quotes).saveAndFlush(pending);
        verify(quotes).save(any(ServiceQuote.class));
    }

    @Test
    void confirmQuoteAssignsTechnicianAndRejectsOtherPendingQuotes() {
        ServiceRequest request = request(RequestStatus.QUOTE_PENDING);
        User otherTechnician = user(Role.TECHNICIAN, "Otro técnico");
        ServiceQuote selected = quote(request, technician, QuoteStatus.PENDING);
        ServiceQuote other = quote(request, otherTechnician, QuoteStatus.PENDING);
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(quotes.findById(selected.getId())).thenReturn(Optional.of(selected));
        when(quotes.findByServiceRequestIdAndStatus(request.getId(), QuoteStatus.PENDING))
                .thenReturn(List.of(selected, other));
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of());

        var response = service.confirmQuote(request.getId(), selected.getId(), client);

        assertThat(response.status()).isEqualTo(RequestStatus.QUOTE_ACCEPTED);
        assertThat(response.technicianId()).isEqualTo(technician.getId());
        assertThat(request.getFinalPrice()).isEqualByComparingTo(selected.getPrice());
        assertThat(selected.getStatus()).isEqualTo(QuoteStatus.ACCEPTED);
        assertThat(other.getStatus()).isEqualTo(QuoteStatus.REJECTED);
        ArgumentCaptor<Object> eventsCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events, org.mockito.Mockito.times(2)).publishEvent(eventsCaptor.capture());
        assertThat(eventsCaptor.getAllValues()).extracting(value -> ((UserNotificationEvent) value).type())
                .containsExactlyInAnyOrder(NotificationType.QUOTE_REJECTED, NotificationType.QUOTE_ACCEPTED);
    }

    @Test
    void rejectQuoteKeepsRequestAvailableAndNotifiesTechnician() {
        ServiceRequest request = request(RequestStatus.QUOTE_PENDING);
        ServiceQuote rejected = quote(request, technician, QuoteStatus.PENDING);
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(quotes.findById(rejected.getId())).thenReturn(Optional.of(rejected));
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of());

        var response = service.rejectQuote(request.getId(), rejected.getId(), client);

        assertThat(response.status()).isEqualTo(QuoteStatus.REJECTED);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.QUOTE_PENDING);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(((UserNotificationEvent) event.getValue()).type()).isEqualTo(NotificationType.QUOTE_REJECTED);
    }

    @Test
    void clientCanCancelAssignedRequestAndNotifyTechnician() {
        ServiceRequest request = request(RequestStatus.QUOTE_ACCEPTED);
        request.setTechnician(technician);
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of());

        var response = service.updateStatus(request.getId(), RequestStatus.CANCELLED, client);

        assertThat(response.status()).isEqualTo(RequestStatus.CANCELLED);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        UserNotificationEvent notification = (UserNotificationEvent) event.getValue();
        assertThat(notification.userId()).isEqualTo(technician.getId());
        assertThat(notification.type()).isEqualTo(NotificationType.SERVICE_STATUS_CHANGED);
    }

    @Test
    void technicianStatusTransitionCompletesServiceAndIncrementsBothCounters() {
        ServiceRequest request = request(RequestStatus.IN_PROGRESS);
        request.setTechnician(technician);
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of());

        var response = service.updateStatus(request.getId(), RequestStatus.COMPLETED, technician);

        assertThat(response.status()).isEqualTo(RequestStatus.COMPLETED);
        assertThat(client.getCompletedServicesCount()).isEqualTo(1);
        assertThat(technician.getCompletedServicesCount()).isEqualTo(1);
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(((UserNotificationEvent) event.getValue()).type())
                .isEqualTo(NotificationType.SERVICE_COMPLETED);
    }

    @Test
    void invalidTechnicianStatusTransitionIsRejectedWithoutMutation() {
        ServiceRequest request = request(RequestStatus.QUOTE_ACCEPTED);
        request.setTechnician(technician);
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.updateStatus(request.getId(), RequestStatus.COMPLETED, technician))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Invalid service request status transition");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.QUOTE_ACCEPTED);
        verify(events, never()).publishEvent(any());
    }

    @Test
    void technicianPaymentConfirmationCreatesCommissionedPaymentAndClosesRequest() {
        ServiceRequest request = request(RequestStatus.COMPLETED);
        request.setTechnician(technician);
        request.setFinalPrice(new BigDecimal("200000"));
        request.setRequestedPaymentMethod(PaymentMethod.CASH);
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(payments.existsByServiceRequestId(request.getId())).thenReturn(false);
        when(parameters.platformCommissionPercentage()).thenReturn(new BigDecimal("5"));
        when(feeCalculator.fee(new BigDecimal("200000"), new BigDecimal("5")))
                .thenReturn(new BigDecimal("10000"));
        when(feeCalculator.technicianAmount(new BigDecimal("200000"), new BigDecimal("5")))
                .thenReturn(new BigDecimal("190000"));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of());

        var response = service.technicianComplete(request.getId(), true, PaymentMethod.CASH, null, technician);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(payment.capture());
        assertThat(payment.getValue().getPlatformFee()).isEqualByComparingTo("10000");
        assertThat(payment.getValue().getTechnicianAmount()).isEqualByComparingTo("190000");
        assertThat(response.status()).isEqualTo(RequestStatus.PAID);
        assertThat(client.getPaidServicesCount()).isEqualTo(1);
        assertThat(technician.getPaidServicesCount()).isEqualTo(1);
    }

    @Test
    void duplicatePaymentIsRejectedBeforeCountersChange() {
        ServiceRequest request = request(RequestStatus.COMPLETED);
        request.setTechnician(technician);
        request.setFinalPrice(new BigDecimal("200000"));
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(payments.existsByServiceRequestId(request.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.technicianComplete(
                request.getId(), true, PaymentMethod.CASH, null, technician))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Service request is already paid");
        assertThat(client.getPaidServicesCount()).isZero();
        assertThat(technician.getPaidServicesCount()).isZero();
        verify(payments, never()).save(any());
    }

    @Test
    void referralRewardWaivesPlatformCommission() {
        ServiceRequest request = request(RequestStatus.COMPLETED);
        request.setTechnician(technician);
        request.setFinalPrice(new BigDecimal("200000"));
        request.setRequestedPaymentMethod(PaymentMethod.CASH);
        com.tecngo.referrals.entity.ReferralReward reward =
                com.tecngo.referrals.entity.ReferralReward.builder().id(UUID.randomUUID()).build();
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(payments.existsByServiceRequestId(request.getId())).thenReturn(false);
        when(parameters.platformCommissionPercentage()).thenReturn(new BigDecimal("5"));
        when(referrals.useAvailableReward(technician, request, new BigDecimal("5"))).thenReturn(reward);
        when(feeCalculator.fee(new BigDecimal("200000"), BigDecimal.ZERO)).thenReturn(BigDecimal.ZERO);
        when(feeCalculator.technicianAmount(new BigDecimal("200000"), BigDecimal.ZERO))
                .thenReturn(new BigDecimal("200000"));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of());

        service.technicianComplete(request.getId(), true, null, null, technician);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(payment.capture());
        assertThat(payment.getValue().isCommissionWaived()).isTrue();
        assertThat(payment.getValue().getPlatformCommissionPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(payment.getValue().getTechnicianAmount()).isEqualByComparingTo("200000");
        assertThat(payment.getValue().getReferralReward()).isEqualTo(reward);
    }

    @Test
    void missingPaymentCreatesDisputeReportWithoutPayment() {
        ServiceRequest request = request(RequestStatus.COMPLETED);
        request.setTechnician(technician);
        request.setFinalPrice(new BigDecimal("200000"));
        when(requests.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(anyList())).thenReturn(List.of());
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any())).thenReturn(List.of());

        var response = service.technicianComplete(request.getId(), false, null,
                "  El cliente no realizó el pago  ", technician);

        ArgumentCaptor<UserReport> report = ArgumentCaptor.forClass(UserReport.class);
        verify(reports).save(report.capture());
        assertThat(report.getValue().getReason()).isEqualTo(ReportReason.PAYMENT_ISSUE);
        assertThat(report.getValue().getDescription()).isEqualTo("El cliente no realizó el pago");
        assertThat(response.status()).isEqualTo(RequestStatus.PAYMENT_DISPUTE);
        verify(payments, never()).save(any());
    }

    @Test
    void detailExposesExactLocationOnlyToParticipantAndFiltersNonApprovedImages() {
        ServiceRequest request = request(RequestStatus.QUOTE_ACCEPTED);
        request.setTechnician(technician);
        ServiceRequestImage approved = image(request, "/private/approved.jpg", ModerationStatus.APPROVED);
        ServiceRequestImage rejected = image(request, "/private/rejected.jpg", ModerationStatus.REJECTED);
        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
        when(images.findByServiceRequestIdInOrderByCreatedAtAsc(List.of(request.getId())))
                .thenReturn(List.of(approved, rejected));
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(List.of(technician.getId())))
                .thenReturn(List.of(TechnicianProfile.builder()
                        .user(technician).categories(Set.of(category)).build()));

        var response = service.detail(request.getId(), client);

        assertThat(response.locationPrecision()).isEqualTo(com.tecngo.geolocation.LocationPrecision.EXACT);
        assertThat(response.latitude()).isEqualTo(request.getLatitude());
        assertThat(response.images()).extracting(item -> item.imageUrl())
                .containsExactly("/private/approved.jpg");
        assertThat(response.technicianCategories()).containsExactly("Electricista");
    }

    @Test
    void quoteListingLoadsTechnicianCategoriesInOneBatch() {
        ServiceRequest request = request(RequestStatus.QUOTE_PENDING);
        User secondTechnician = user(Role.TECHNICIAN, "Segundo técnico");
        ServiceQuote first = quote(request, technician, QuoteStatus.PENDING);
        ServiceQuote second = quote(request, secondTechnician, QuoteStatus.PENDING);
        TechnicianProfile firstProfile = TechnicianProfile.builder()
                .user(technician).categories(Set.of(category)).build();
        ServiceCategory plumbing = ServiceCategory.builder()
                .id(UUID.randomUUID()).name("Plomero").active(true).build();
        TechnicianProfile secondProfile = TechnicianProfile.builder()
                .user(secondTechnician).categories(Set.of(plumbing)).build();
        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
        when(quotes.findByServiceRequestIdOrderByCreatedAtAsc(request.getId()))
                .thenReturn(List.of(first, second));
        when(technicianProfileRepository.findWithCategoriesByUserIdIn(any()))
                .thenReturn(List.of(firstProfile, secondProfile));

        var response = service.quotes(request.getId(), client);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).technicianCategories()).containsExactly("Electricista");
        assertThat(response.get(1).technicianCategories()).containsExactly("Plomero");
        verify(technicianProfileRepository, org.mockito.Mockito.times(1))
                .findWithCategoriesByUserIdIn(any());
    }

    private User user(Role role, String name) {
        return User.builder()
                .id(UUID.randomUUID())
                .role(role)
                .fullName(name)
                .email(name.toLowerCase().replace(" ", ".") + "@tecngo.test")
                .averageRating(new BigDecimal("5.00"))
                .completedServicesCount(0)
                .paidServicesCount(0)
                .documentsVerified(true)
                .build();
    }

    private ServiceRequest request(RequestStatus status) {
        return ServiceRequest.builder()
                .id(UUID.randomUUID())
                .client(client)
                .category(category)
                .city(city)
                .description("Trabajo eléctrico")
                .address("Zona, Villavicencio")
                .latitude(4.14)
                .longitude(-73.63)
                .estimatedPrice(new BigDecimal("100000"))
                .requestedPaymentMethod(PaymentMethod.CASH)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private ServiceQuote quote(ServiceRequest request, User owner, QuoteStatus status) {
        return ServiceQuote.builder()
                .id(UUID.randomUUID())
                .serviceRequest(request)
                .technician(owner)
                .price(new BigDecimal("120000"))
                .description("Oferta")
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();
    }

    private ServiceRequestImage image(ServiceRequest request, String url, ModerationStatus status) {
        ContentAsset asset = ContentAsset.builder()
                .id(UUID.randomUUID())
                .moderationStatus(status)
                .build();
        return ServiceRequestImage.builder()
                .id(UUID.randomUUID())
                .serviceRequest(request)
                .imageUrl(url)
                .publicId(UUID.randomUUID().toString())
                .contentAsset(asset)
                .createdAt(Instant.now())
                .build();
    }
}
