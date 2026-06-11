package com.tecngo.payment_proofs.controller;
import com.tecngo.payment_proofs.dto.*;
import com.tecngo.payment_proofs.service.PaymentProofService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/v1/admin/payment-proofs") @RequiredArgsConstructor
public class AdminPaymentProofController {
    private final PaymentProofService service;
    @GetMapping("/pending") public List<PaymentProofResponse> pending(@AuthenticationPrincipal User user) {
        return service.pending(user);
    }
    @PutMapping("/{id}/approve") public PaymentProofResponse approve(@PathVariable UUID id,
            @RequestBody(required = false) ReviewPaymentProofRequest request, @AuthenticationPrincipal User user) {
        return service.review(id, true, request == null ? null : request.comment(), user);
    }
    @PutMapping("/{id}/reject") public PaymentProofResponse reject(@PathVariable UUID id,
            @Valid @RequestBody ReviewPaymentProofRequest request, @AuthenticationPrincipal User user) {
        return service.review(id, false, request.comment(), user);
    }
}
