package com.tecngo.service_requests.controller;

import com.tecngo.service_requests.dto.*;
import com.tecngo.service_requests.service.ServiceRequestService;
import com.tecngo.users.entity.User;
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
    public List<ServiceRequestResponse> my(@AuthenticationPrincipal User user) {
        return service.mine(user);
    }

    @GetMapping("/my-assigned")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<ServiceRequestResponse> myAssigned(@AuthenticationPrincipal User user) {
        return service.mine(user);
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
            @RequestParam(defaultValue = "10") double radiusKm,
            @AuthenticationPrincipal User user) {
        return service.available(user, radiusKm);
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
}
