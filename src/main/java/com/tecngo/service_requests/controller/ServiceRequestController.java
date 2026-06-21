package com.tecngo.service_requests.controller;

import com.tecngo.service_requests.dto.*;
import com.tecngo.service_requests.service.ServiceRequestService;
import com.tecngo.users.entity.User;
import com.tecngo.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {
    private final ServiceRequestService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public ServiceRequestResponse create(@Valid @RequestBody CreateServiceRequest request,
                                         @AuthenticationPrincipal User user) {
        return service.create(request, user);
    }

    @GetMapping("/mine")
    public List<ServiceRequestResponse> mine(@AuthenticationPrincipal User user) {
        return service.mine(user);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ServiceRequestResponse> my(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal User user) {
        return service.clientRequests(user, activeOnly);
    }

    @GetMapping("/my/page")
    @PreAuthorize("hasRole('CLIENT')")
    public PageResponse<ServiceRequestResponse> myPage(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return PageResponse.from(service.clientRequestsPage(user, activeOnly, page, size));
    }

    @GetMapping("/my/history")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ServiceRequestResponse> myHistory(@AuthenticationPrincipal User user) {
        return service.clientHistory(user);
    }

    @GetMapping("/my/history/page")
    @PreAuthorize("hasRole('CLIENT')")
    public PageResponse<ServiceRequestResponse> myHistoryPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return PageResponse.from(service.clientHistoryPage(user, page, size));
    }

    @GetMapping("/my-assigned")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<ServiceRequestResponse> myAssigned(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal User user) {
        return service.assignedRequests(user, activeOnly);
    }

    @GetMapping("/my-assigned/page")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PageResponse<ServiceRequestResponse> myAssignedPage(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return PageResponse.from(service.assignedRequestsPage(user, activeOnly, page, size));
    }

    @GetMapping("/my-assigned/history")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<ServiceRequestResponse> myAssignedHistory(@AuthenticationPrincipal User user) {
        return service.assignedHistory(user);
    }

    @GetMapping("/my-assigned/history/page")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PageResponse<ServiceRequestResponse> myAssignedHistoryPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return PageResponse.from(service.assignedHistoryPage(user, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'TECHNICIAN')")
    public ServiceRequestResponse detail(@PathVariable java.util.UUID id,
                                         @AuthenticationPrincipal User user) {
        return service.detail(id, user);
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<ServiceRequestResponse> available(
            @RequestParam(required = false) java.util.UUID cityId,
            @RequestParam(required = false) java.util.UUID categoryId,
            @RequestParam(required = false) Boolean useRadius,
            @RequestParam(required = false) Double radiusKm,
            @AuthenticationPrincipal User user) {
        return service.available(user, cityId, categoryId, useRadius, radiusKm);
    }

    @GetMapping("/available/page")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public PageResponse<ServiceRequestResponse> availablePage(
            @RequestParam(required = false) java.util.UUID cityId,
            @RequestParam(required = false) java.util.UUID categoryId,
            @RequestParam(required = false) Boolean useRadius,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return PageResponse.from(service.availablePage(
                user, cityId, categoryId, useRadius, radiusKm, page, size));
    }

    @PutMapping("/{id}/quote")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ServiceQuoteResponse quote(@PathVariable java.util.UUID id,
                                      @Valid @RequestBody QuoteRequest request,
                                      @AuthenticationPrincipal User user) {
        return service.quote(id, request.technicianPrice(), request.description(), user);
    }

    @GetMapping("/{id}/quotes")
    @PreAuthorize("hasRole('CLIENT')")
    public List<ServiceQuoteResponse> quotes(@PathVariable java.util.UUID id,
                                             @AuthenticationPrincipal User user) {
        return service.quotes(id, user);
    }

    @PutMapping("/{id}/confirm-quote")
    @PreAuthorize("hasRole('CLIENT')")
    public ServiceRequestResponse confirmQuote(@PathVariable java.util.UUID id,
                                               @Valid @RequestBody ConfirmQuoteRequest request,
                                               @AuthenticationPrincipal User user) {
        return service.confirmQuote(id, request.quoteId(), user);
    }

    @PutMapping("/{id}/quotes/{quoteId}/reject")
    @PreAuthorize("hasRole('CLIENT')")
    public ServiceQuoteResponse rejectQuote(@PathVariable java.util.UUID id,
                                            @PathVariable java.util.UUID quoteId,
                                            @AuthenticationPrincipal User user) {
        return service.rejectQuote(id, quoteId, user);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CLIENT', 'TECHNICIAN')")
    public ServiceRequestResponse updateStatus(@PathVariable java.util.UUID id,
                                               @Valid @RequestBody UpdateRequestStatus request,
                                               @AuthenticationPrincipal User user) {
        return service.updateStatus(id, request.status(), user);
    }

    @PostMapping("/{id}/technician-complete")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ServiceRequestResponse technicianComplete(@PathVariable java.util.UUID id,
                                                     @Valid @RequestBody TechnicianCompleteRequest request,
                                                     @AuthenticationPrincipal User user) {
        return service.technicianComplete(id, request.paymentReceived(), request.paymentMethod(),
                request.comment(), user);
    }
}
