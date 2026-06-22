package com.tecngo.service_requests.service;

import com.tecngo.service_requests.entity.QuoteStatus;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceQuote;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceQuoteRepository;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.users.service.UserAccessService;
import com.tecngo.legal.service.LegalService;
import com.tecngo.verification.service.EmailVerificationService;
import com.tecngo.users.entity.ActiveMode;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestSelfActionGuardTest {
    @Mock
    private ServiceRequestRepository requests;
    @Mock
    private ServiceQuoteRepository quotes;
    @Mock
    private UserAccessService userAccess;
    @Mock
    private LegalService legal;
    @Mock
    private EmailVerificationService emailVerification;
    @Mock
    private com.tecngo.technicians.service.TechnicianProfileService technicianProfiles;
    @Mock
    private com.tecngo.technicians.repository.TechnicianProfileRepository technicianProfileRepository;
    @Mock
    private com.tecngo.service_requests.repository.ServiceRequestImageRepository images;
    @Mock
    private com.tecngo.system_parameters.service.SystemParameterService parameters;
    @Mock
    private com.tecngo.technician_wallet.service.TechnicianWalletService wallets;
    @Mock
    private org.springframework.context.ApplicationEventPublisher events;
    @Mock
    private com.tecngo.geolocation.HaversineDistance distance;
    private ServiceQuoteService service;

    @BeforeEach
    void setUp() {
        ServiceRequestAccessPolicy access = new ServiceRequestAccessPolicy(userAccess, legal);
        ServiceRequestAssembler assembler = new ServiceRequestAssembler(images, technicianProfileRepository);
        ServiceRequestNotifier notifier = new ServiceRequestNotifier(events, technicianProfileRepository, distance);
        service = new ServiceQuoteService(
                requests, quotes, technicianProfiles, emailVerification, parameters, wallets,
                access, assembler, notifier);
    }

    @Test
    void dualRoleUserCannotQuoteOwnRequest() {
        User user = dualRoleUser(ActiveMode.TECHNICIAN);
        UUID requestId = UUID.randomUUID();
        ServiceRequest request = ServiceRequest.builder()
                .id(requestId)
                .client(user)
                .status(RequestStatus.QUOTE_PENDING)
                .build();
        when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.quote(requestId, new BigDecimal("100000"), "Trabajo", user))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You cannot quote your own service request");
    }

    @Test
    void dualRoleUserCannotAcceptOwnQuote() {
        User user = dualRoleUser(ActiveMode.CLIENT);
        UUID requestId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();
        ServiceRequest request = ServiceRequest.builder()
                .id(requestId)
                .client(user)
                .status(RequestStatus.QUOTE_PENDING)
                .build();
        ServiceQuote quote = ServiceQuote.builder()
                .id(quoteId)
                .serviceRequest(request)
                .technician(user)
                .status(QuoteStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        when(quotes.findById(quoteId)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> service.confirmQuote(requestId, quoteId, user))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You cannot accept a quote created by your own account");
    }

    private User dualRoleUser(ActiveMode mode) {
        return User.builder()
                .id(UUID.randomUUID())
                .role(Role.CLIENT)
                .roles(new LinkedHashSet<>(List.of(Role.CLIENT, Role.TECHNICIAN)))
                .activeMode(mode)
                .build();
    }
}
