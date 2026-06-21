package com.tecngo.technician_wallet.controller;

import com.tecngo.technician_wallet.dto.*;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/technicians/me/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TECHNICIAN')")
public class TechnicianWalletController {
    private final TechnicianWalletService service;

    @GetMapping
    public TechnicianWalletResponse wallet(@AuthenticationPrincipal User user) {
        return service.mine(user);
    }

    @GetMapping("/transactions")
    public List<TechnicianWalletTransactionResponse> transactions(@AuthenticationPrincipal User user) {
        return service.myTransactions(user);
    }

    @PostMapping("/recharge")
    public RechargeResponse recharge(@Valid @RequestBody RechargeRequest request,
                                     @AuthenticationPrincipal User user) {
        return service.createRecharge(user, request.amount(),
                "MOBILE".equalsIgnoreCase(request.platform()));
    }

    @PutMapping("/recharges/{id}/transaction")
    public void attachTransaction(@PathVariable UUID id,
                                  @Valid @RequestBody WompiTransactionRequest request,
                                  @AuthenticationPrincipal User user) {
        service.attachAndReconcile(id, request.transactionId(), user);
    }

    @PutMapping("/recharges/transaction")
    public void attachTransaction(@Valid @RequestBody WompiTransactionRequest request,
                                  @AuthenticationPrincipal User user) {
        service.attachAndReconcile(request.transactionId(), user);
    }
}
