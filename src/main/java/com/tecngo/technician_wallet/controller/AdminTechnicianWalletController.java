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
@RequestMapping("/v1/admin/technicians")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTechnicianWalletController {
    private final TechnicianWalletService service;

    @GetMapping("/wallets")
    public List<TechnicianWalletResponse> wallets(@AuthenticationPrincipal User admin) {
        return service.adminWallets(admin);
    }

    @GetMapping("/{id}/wallet")
    public TechnicianWalletResponse wallet(@PathVariable UUID id, @AuthenticationPrincipal User admin) {
        return service.adminWallet(id, admin);
    }

    @PostMapping("/{id}/wallet/adjustment")
    public TechnicianWalletTransactionResponse adjustment(@PathVariable UUID id,
                                                          @Valid @RequestBody AdminWalletAdjustmentRequest request,
                                                          @AuthenticationPrincipal User admin) {
        return service.adminAdjustment(id, request.amount(), request.comment(), admin);
    }
}
