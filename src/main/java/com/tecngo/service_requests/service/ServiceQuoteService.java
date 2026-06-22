package com.tecngo.service_requests.service;

import com.tecngo.service_requests.dto.ServiceQuoteResponse;
import com.tecngo.service_requests.dto.ServiceRequestResponse;
import com.tecngo.service_requests.entity.QuoteStatus;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.service_requests.entity.ServiceQuote;
import com.tecngo.service_requests.entity.ServiceRequest;
import com.tecngo.service_requests.repository.ServiceQuoteRepository;
import com.tecngo.service_requests.repository.ServiceRequestRepository;
import com.tecngo.shared.exception.ConflictException;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.shared.exception.NotFoundException;
import com.tecngo.system_parameters.service.SystemParameterService;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import com.tecngo.technicians.service.TechnicianProfileService;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import com.tecngo.verification.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceQuoteService {
    private final ServiceRequestRepository requests;
    private final ServiceQuoteRepository quotes;
    private final TechnicianProfileService technicianProfiles;
    private final EmailVerificationService emailVerification;
    private final SystemParameterService parameters;
    private final TechnicianWalletService wallets;
    private final ServiceRequestAccessPolicy access;
    private final ServiceRequestAssembler assembler;
    private final ServiceRequestNotifier notifier;

    @Transactional
    public ServiceQuoteResponse quote(UUID id, BigDecimal technicianPrice,
                                      String description, User technician) {
        access.requireRole(technician, Role.TECHNICIAN);
        access.requireCriticalAccess(technician);
        emailVerification.requireVerified(technician);
        ServiceRequest request = lockedRequest(id);
        access.requireDifferentUser(request.getClient(), technician,
                "You cannot quote your own service request");
        if (request.getStatus() != RequestStatus.QUOTE_PENDING) {
            throw new ConflictException("Service request is no longer available");
        }
        var profile = technicianProfiles.approvedProfile(technician);
        wallets.requireCanQuote(technician);
        boolean supportsCategory = profile.getCategories().stream()
                .anyMatch(category -> category.getId().equals(request.getCategory().getId()));
        if (!supportsCategory) throw new ForbiddenException("Technician does not support this category");
        expirePreviousOffer(id, technician);
        if (technicianPrice == null || technicianPrice.signum() <= 0) {
            throw new IllegalArgumentException("Quote price must be greater than zero");
        }
        ServiceQuote quote = quotes.save(ServiceQuote.builder()
                .serviceRequest(request)
                .technician(technician)
                .price(technicianPrice)
                .description(clean(description))
                .status(QuoteStatus.PENDING)
                .expiresAt(Instant.now().plus(parameters.quoteExpirationMinutes(), ChronoUnit.MINUTES))
                .build());
        notifier.newQuote(request, technician, technicianPrice);
        return assembler.quote(quote);
    }

    @Transactional
    public ServiceRequestResponse confirmQuote(UUID id, UUID quoteId, User client) {
        access.requireRole(client, Role.CLIENT);
        access.requireCriticalAccess(client);
        ServiceRequest request = lockedRequest(id);
        access.requireClientOwner(request, client);
        if (request.getStatus() != RequestStatus.QUOTE_PENDING || request.getTechnician() != null) {
            throw new ConflictException("Service request no longer accepts quotes");
        }
        ServiceQuote selected = quotes.findById(quoteId)
                .orElseThrow(() -> new NotFoundException("Quote not found"));
        if (!selected.getServiceRequest().getId().equals(id) || selected.getStatus() != QuoteStatus.PENDING) {
            throw new ConflictException("Quote is not available for this service request");
        }
        access.requireDifferentUser(client, selected.getTechnician(),
                "You cannot accept a quote created by your own account");
        requireNotExpired(selected);
        selected.setStatus(QuoteStatus.ACCEPTED);
        selected.setRespondedAt(Instant.now());
        quotes.findByServiceRequestIdAndStatus(id, QuoteStatus.PENDING).stream()
                .filter(item -> !item.getId().equals(selected.getId()))
                .forEach(item -> {
                    item.setStatus(QuoteStatus.REJECTED);
                    item.setRespondedAt(Instant.now());
                    notifier.quoteRejected(request, item.getTechnician());
                });
        request.setTechnician(selected.getTechnician());
        request.setTechnicianPrice(selected.getPrice());
        request.setFinalPrice(selected.getPrice());
        request.setStatus(RequestStatus.QUOTE_ACCEPTED);
        notifier.quoteAccepted(request, selected.getTechnician());
        return assembler.response(request);
    }

    @Transactional(readOnly = true)
    public List<ServiceQuoteResponse> quotes(UUID id, User client) {
        ServiceRequest request = requests.findById(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
        access.requireRole(client, Role.CLIENT);
        access.requireClientOwner(request, client);
        return assembler.quotes(quotes.findByServiceRequestIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public ServiceQuoteResponse rejectQuote(UUID requestId, UUID quoteId, User client) {
        access.requireRole(client, Role.CLIENT);
        access.requireCriticalAccess(client);
        ServiceRequest request = lockedRequest(requestId);
        access.requireClientOwner(request, client);
        ServiceQuote quote = quotes.findById(quoteId)
                .filter(item -> item.getServiceRequest().getId().equals(requestId))
                .orElseThrow(() -> new NotFoundException("Quote not found"));
        if (quote.getStatus() != QuoteStatus.PENDING) {
            throw new ConflictException("Quote is no longer pending");
        }
        requireNotExpired(quote);
        quote.setStatus(QuoteStatus.REJECTED);
        quote.setRespondedAt(Instant.now());
        notifier.quoteRejected(request, quote.getTechnician());
        return assembler.quote(quote);
    }

    private void expirePreviousOffer(UUID requestId, User technician) {
        quotes.findFirstByServiceRequestIdAndTechnicianIdAndStatus(
                requestId, technician.getId(), QuoteStatus.PENDING).ifPresent(pending -> {
            if (pending.getExpiresAt().isAfter(Instant.now())) {
                throw new ConflictException(
                        "You already have a pending quote for this service. Wait for the client response or expiration.");
            }
            pending.setStatus(QuoteStatus.EXPIRED);
            pending.setRespondedAt(Instant.now());
            quotes.saveAndFlush(pending);
        });
    }

    private void requireNotExpired(ServiceQuote quote) {
        if (!quote.getExpiresAt().isAfter(Instant.now())) {
            quote.setStatus(QuoteStatus.EXPIRED);
            quote.setRespondedAt(Instant.now());
            throw new ConflictException("Quote has expired");
        }
    }

    private ServiceRequest lockedRequest(UUID id) {
        return requests.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Service request not found"));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
