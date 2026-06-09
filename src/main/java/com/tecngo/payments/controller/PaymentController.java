package com.tecngo.payments.controller;

import com.tecngo.payments.dto.FinancialSummaryResponse;
import com.tecngo.payments.dto.PaymentResponse;
import com.tecngo.payments.service.PaymentService;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;

    @PostMapping("/v1/service-requests/{id}/payment/cash")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public PaymentResponse payCash(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return service.payCash(id, user);
    }

    @GetMapping("/v1/payments/mine")
    @PreAuthorize("hasRole('CLIENT')")
    public List<PaymentResponse> mine(@AuthenticationPrincipal User user) {
        return service.clientHistory(user);
    }

    @GetMapping("/v1/technicians/me/earnings")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public FinancialSummaryResponse earnings(@AuthenticationPrincipal User user) {
        return service.technicianEarnings(user);
    }

    @GetMapping("/v1/admin/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public FinancialSummaryResponse admin(@AuthenticationPrincipal User user) {
        return service.adminSummary(user);
    }
}
