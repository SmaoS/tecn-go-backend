package com.tecngo.service_requests.service;

import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.payments.repository.PaymentRepository;
import com.tecngo.service_requests.entity.QuoteStatus;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceQuote;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceQuoteRepository;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.services.entity.ServiceCategory;
import com.tecngo.services.repository.ServiceCategoryRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.technicians.entity.TechnicianProfile;
import com.tecngo.technicians.entity.TechnicianStatus;
import com.tecngo.technicians.repository.TechnicianProfileRepository;
import com.tecngo.users.entity.AccountStatus;
import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.OnboardingStep;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.users.entity.VerificationStatus;
import com.tecngo.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.parameters.require-legal-acceptance=false",
        "app.parameters.platform-commission-percentage=0",
        "app.parameters.technician-recharge-enabled=false",
        "app.email.require-verification=false"
})
class ServiceRequestConcurrencyIntegrationTest {
    @Autowired ServiceQuoteService quoteService;
    @Autowired ServiceLifecycleService lifecycle;
    @Autowired ServiceRequestRepository requests;
    @Autowired ServiceQuoteRepository quotes;
    @Autowired PaymentRepository payments;
    @Autowired UserRepository users;
    @Autowired ServiceCategoryRepository categories;
    @Autowired TechnicianProfileRepository technicianProfiles;

    @Test
    void concurrentQuoteAcceptanceAssignsOnlyOneTechnician() throws Exception {
        Fixture fixture = fixture(RequestStatus.QUOTE_PENDING, 2);
        ServiceQuote first = quote(fixture.request(), fixture.technicians().get(0), "120000");
        ServiceQuote second = quote(fixture.request(), fixture.technicians().get(1), "130000");

        List<Throwable> outcomes = race(
                () -> quoteService.confirmQuote(
                        fixture.request().getId(), first.getId(), fixture.client()),
                () -> quoteService.confirmQuote(
                        fixture.request().getId(), second.getId(), fixture.client()));

        assertSingleConflict(outcomes);
        ServiceRequest persisted = requests.findById(fixture.request().getId()).orElseThrow();
        List<ServiceQuote> persistedQuotes =
                quotes.findByServiceRequestIdOrderByCreatedAtAsc(fixture.request().getId());
        assertThat(persisted.getStatus()).isEqualTo(RequestStatus.QUOTE_ACCEPTED);
        assertThat(persisted.getTechnician()).isNotNull();
        assertThat(persistedQuotes).filteredOn(item -> item.getStatus() == QuoteStatus.ACCEPTED).hasSize(1);
        assertThat(persistedQuotes).filteredOn(item -> item.getStatus() == QuoteStatus.REJECTED).hasSize(1);
    }

    @Test
    void concurrentStatusTransitionIsAppliedOnce() throws Exception {
        Fixture fixture = fixture(RequestStatus.QUOTE_ACCEPTED, 1);
        User technician = fixture.technicians().getFirst();
        ServiceRequest request = fixture.request();
        request.setTechnician(technician);
        requests.saveAndFlush(request);

        List<Throwable> outcomes = race(
                () -> lifecycle.updateStatus(request.getId(), RequestStatus.ON_THE_WAY, technician),
                () -> lifecycle.updateStatus(request.getId(), RequestStatus.ON_THE_WAY, technician));

        assertSingleConflict(outcomes);
        assertThat(requests.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(RequestStatus.ON_THE_WAY);
    }

    @Test
    void concurrentPaymentConfirmationCreatesOnePaymentWithoutDuplicatingCounters() throws Exception {
        Fixture fixture = fixture(RequestStatus.COMPLETED, 1);
        User technician = fixture.technicians().getFirst();
        ServiceRequest request = fixture.request();
        request.setTechnician(technician);
        request.setTechnicianPrice(new BigDecimal("150000"));
        request.setFinalPrice(new BigDecimal("150000"));
        requests.saveAndFlush(request);
        fixture.client().setCompletedServicesCount(1);
        technician.setCompletedServicesCount(1);
        users.saveAllAndFlush(List.of(fixture.client(), technician));

        List<Throwable> outcomes = race(
                () -> lifecycle.technicianComplete(
                        request.getId(), true, PaymentMethod.CASH, null, technician),
                () -> lifecycle.technicianComplete(
                        request.getId(), true, PaymentMethod.CASH, null, technician));

        assertSingleConflict(outcomes);
        assertThat(payments.findByServiceRequestId(request.getId())).isPresent();
        assertThat(requests.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(RequestStatus.PAID);
        User persistedClient = users.findById(fixture.client().getId()).orElseThrow();
        User persistedTechnician = users.findById(technician.getId()).orElseThrow();
        assertThat(persistedClient.getCompletedServicesCount()).isEqualTo(1);
        assertThat(persistedTechnician.getCompletedServicesCount()).isEqualTo(1);
        assertThat(persistedClient.getPaidServicesCount()).isEqualTo(1);
        assertThat(persistedTechnician.getPaidServicesCount()).isEqualTo(1);
    }

    private Fixture fixture(RequestStatus status, int technicianCount) {
        String suffix = UUID.randomUUID().toString();
        ServiceCategory category = categories.saveAndFlush(ServiceCategory.builder()
                .name("Concurrency " + suffix)
                .slug("concurrency-" + suffix)
                .active(true)
                .build());
        User client = users.saveAndFlush(user(Role.CLIENT, "Client " + suffix, suffix + "@client.test"));
        List<User> technicians = java.util.stream.IntStream.range(0, technicianCount)
                .mapToObj(index -> users.saveAndFlush(user(
                        Role.TECHNICIAN,
                        "Technician " + index + " " + suffix,
                        suffix + "-" + index + "@technician.test")))
                .toList();
        technicians.forEach(technician -> technicianProfiles.saveAndFlush(TechnicianProfile.builder()
                .user(technician)
                .documentNumber("DOC-" + UUID.randomUUID())
                .phone("300" + Math.abs(UUID.randomUUID().hashCode()))
                .categories(new HashSet<>(List.of(category)))
                .description("Experiencia técnica comprobada para pruebas concurrentes.")
                .status(TechnicianStatus.APPROVED)
                .available(true)
                .build()));
        ServiceRequest request = requests.saveAndFlush(ServiceRequest.builder()
                .client(client)
                .category(category)
                .description("Solicitud para validar concurrencia")
                .address("Villavicencio")
                .latitude(4.14)
                .longitude(-73.63)
                .estimatedPrice(new BigDecimal("100000"))
                .requestedPaymentMethod(PaymentMethod.CASH)
                .status(status)
                .build());
        return new Fixture(client, technicians, request);
    }

    private User user(Role role, String name, String email) {
        return User.builder()
                .fullName(name)
                .email(email)
                .password("test-password")
                .role(role)
                .activeMode(ActiveMode.valueOf(role.name()))
                .verificationStatus(VerificationStatus.VERIFIED)
                .emailVerified(true)
                .documentsVerified(true)
                .documentPhotoUrl("/private/document-" + UUID.randomUUID())
                .workExperienceDescription(
                        "Experiencia laboral suficiente para operar dentro de TecnGo.")
                .accountStatus(AccountStatus.ACTIVE)
                .onboardingCompleted(true)
                .onboardingStep(OnboardingStep.COMPLETED)
                .build();
    }

    private ServiceQuote quote(ServiceRequest request, User technician, String price) {
        return quotes.saveAndFlush(ServiceQuote.builder()
                .serviceRequest(request)
                .technician(technician)
                .price(new BigDecimal(price))
                .description("Cotización concurrente")
                .status(QuoteStatus.PENDING)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build());
    }

    private List<Throwable> race(ThrowingRunnable first, ThrowingRunnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Throwable> firstResult = executor.submit(() -> runReady(ready, start, first));
            Future<Throwable> secondResult = executor.submit(() -> runReady(ready, start, second));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return java.util.Arrays.asList(firstResult.get(15, TimeUnit.SECONDS),
                    secondResult.get(15, TimeUnit.SECONDS));
        }
    }

    private Throwable runReady(
            CountDownLatch ready,
            CountDownLatch start,
            ThrowingRunnable operation
    ) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            operation.run();
            return null;
        } catch (Throwable exception) {
            return exception;
        }
    }

    private void assertSingleConflict(List<Throwable> outcomes) {
        assertThat(outcomes).filteredOn(java.util.Objects::isNull).hasSize(1);
        assertThat(outcomes).filteredOn(java.util.Objects::nonNull)
                .singleElement()
                .isInstanceOf(ConflictException.class);
    }

    private record Fixture(User client, List<User> technicians, ServiceRequest request) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
