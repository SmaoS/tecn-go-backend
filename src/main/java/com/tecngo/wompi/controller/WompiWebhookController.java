package com.tecngo.wompi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tecngo.technician_wallet.service.TechnicianWalletService;
import com.tecngo.wompi.service.WompiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/wompi")
@RequiredArgsConstructor
public class WompiWebhookController {
    private final WompiPaymentService wompi;
    private final TechnicianWalletService wallets;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handle(@RequestBody JsonNode body,
                       @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {
        wompi.verifyWebhook(body, checksum);
        JsonNode transaction = body.path("data").path("transaction");
        String reference = transaction.path("reference").asText(null);
        String transactionId = transaction.path("id").asText(null);
        String status = transaction.path("status").asText("");
        if (reference == null || !reference.startsWith("TECNGO-TECH-")) return;
        if ("APPROVED".equalsIgnoreCase(status)) {
            wallets.approveRecharge(reference, transactionId);
        } else if ("DECLINED".equalsIgnoreCase(status)
                || "VOIDED".equalsIgnoreCase(status)
                || "ERROR".equalsIgnoreCase(status)) {
            wallets.rejectRecharge(reference, transactionId);
        }
    }
}
