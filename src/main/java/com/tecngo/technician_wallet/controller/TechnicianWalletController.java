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
        return service.createRecharge(user, request.amount());
    }
}
