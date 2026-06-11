package com.tecngo.payment_proofs.controller;
import com.tecngo.payment_proofs.dto.PaymentProofResponse;
import com.tecngo.payment_proofs.entity.ProofPaymentMethod;
import com.tecngo.payment_proofs.service.PaymentProofService;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/v1/service-requests/{requestId}/payment-proofs") @RequiredArgsConstructor
public class PaymentProofController {
    private final PaymentProofService service;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public PaymentProofResponse upload(@PathVariable UUID requestId, @RequestPart("file") MultipartFile file,
            @RequestParam BigDecimal amount, @RequestParam ProofPaymentMethod paymentMethod,
            @AuthenticationPrincipal User user) { return service.upload(requestId, amount, paymentMethod, file, user); }
    @GetMapping public List<PaymentProofResponse> list(@PathVariable UUID requestId,
            @AuthenticationPrincipal User user) { return service.list(requestId, user); }
}
