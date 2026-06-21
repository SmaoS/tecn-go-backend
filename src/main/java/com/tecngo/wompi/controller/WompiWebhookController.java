package com.tecngo.wompi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tecngo.outbox.service.OutboxPublisher;
import com.tecngo.wompi.service.WompiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/wompi")
@RequiredArgsConstructor
public class WompiWebhookController {
    private final WompiPaymentService wompi;
    private final OutboxPublisher outbox;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handle(@RequestBody JsonNode body,
                       @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {
        wompi.verifyWebhook(body, checksum);
        JsonNode transaction = body.path("data").path("transaction");
        String reference = transaction.path("reference").asText(null);
        if (reference == null || !reference.startsWith("TECNGO-TECH-")) return;
        var snapshot = wompi.snapshot(transaction);
        outbox.publishExternal(
                OutboxPublisher.WOMPI_TRANSACTION_UPDATED,
                "TECHNICIAN_RECHARGE",
                reference,
                wompi.webhookEventKey(body, checksum),
                snapshot);
    }
}
