package com.tecngo.service_requests.service;

import com.tecngo.payments.entity.PaymentMethod;
import com.tecngo.service_requests.dto.CreateServiceRequest;
import com.tecngo.service_requests.dto.ServiceQuoteResponse;
import com.tecngo.service_requests.dto.ServiceRequestResponse;
import com.tecngo.service_requests.entity.RequestStatus;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestCommandService commands;
    private final ServiceRequestQueryService queries;
    private final ServiceQuoteService quoteService;
    private final ServiceLifecycleService lifecycle;

    public ServiceRequestResponse create(CreateServiceRequest request, User client) {
        return commands.create(request, client);
    }

    public List<ServiceRequestResponse> mine(User user) {
        return queries.mine(user);
    }

    public List<ServiceRequestResponse> clientRequests(User user, boolean activeOnly) {
        return queries.clientRequests(user, activeOnly);
    }

    public Page<ServiceRequestResponse> clientRequestsPage(User user, boolean activeOnly, int page, int size) {
        return queries.clientRequestsPage(user, activeOnly, page, size);
    }

    public List<ServiceRequestResponse> clientHistory(User user) {
        return queries.clientHistory(user);
    }

    public Page<ServiceRequestResponse> clientHistoryPage(User user, int page, int size) {
        return queries.clientHistoryPage(user, page, size);
    }

    public List<ServiceRequestResponse> assignedRequests(User user, boolean activeOnly) {
        return queries.assignedRequests(user, activeOnly);
    }

    public Page<ServiceRequestResponse> assignedRequestsPage(User user, boolean activeOnly, int page, int size) {
        return queries.assignedRequestsPage(user, activeOnly, page, size);
    }

    public List<ServiceRequestResponse> assignedHistory(User user) {
        return queries.assignedHistory(user);
    }

    public Page<ServiceRequestResponse> assignedHistoryPage(User user, int page, int size) {
        return queries.assignedHistoryPage(user, page, size);
    }

    public ServiceRequestResponse detail(UUID id, User user) {
        return queries.detail(id, user);
    }

    public List<ServiceRequestResponse> available(User technician, UUID cityId, UUID categoryId,
                                                  Boolean useRadius, Double radiusKm) {
        return queries.available(technician, cityId, categoryId, useRadius, radiusKm);
    }

    public Page<ServiceRequestResponse> availablePage(User technician, UUID cityId, UUID categoryId,
                                                      Boolean useRadius, Double radiusKm,
                                                      int page, int size) {
        return queries.availablePage(technician, cityId, categoryId, useRadius, radiusKm, page, size);
    }

    public ServiceQuoteResponse quote(UUID id, BigDecimal price, String description, User technician) {
        return quoteService.quote(id, price, description, technician);
    }

    public ServiceRequestResponse confirmQuote(UUID id, UUID quoteId, User client) {
        return quoteService.confirmQuote(id, quoteId, client);
    }

    public List<ServiceQuoteResponse> quotes(UUID id, User client) {
        return quoteService.quotes(id, client);
    }

    public ServiceQuoteResponse rejectQuote(UUID requestId, UUID quoteId, User client) {
        return quoteService.rejectQuote(requestId, quoteId, client);
    }

    public ServiceRequestResponse updateStatus(UUID id, RequestStatus status, User user) {
        return lifecycle.updateStatus(id, status, user);
    }

    public ServiceRequestResponse technicianComplete(UUID id, boolean paymentReceived,
                                                     PaymentMethod paymentMethod, String comment,
                                                     User technician) {
        return lifecycle.technicianComplete(id, paymentReceived, paymentMethod, comment, technician);
    }
}
