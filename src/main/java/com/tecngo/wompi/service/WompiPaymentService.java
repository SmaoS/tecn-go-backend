package com.tecngo.wompi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.wompi.config.WompiProperties;
import com.tecngo.wompi.dto.WompiTransactionSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class WompiPaymentService {
    private final WompiProperties properties;
    private final RestClient restClient;

    public WompiPaymentService(WompiProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.apiBaseUrl()).build();
    }

    public String checkoutUrl(String reference, BigDecimal amount, String currency) {
        return checkoutUrl(reference, amount, currency, false);
    }

    public String checkoutUrl(String reference, BigDecimal amount, String currency, boolean mobile) {
        if (!properties.configuredForCheckout()) {
            throw new IllegalStateException("Wompi checkout keys are not configured");
        }
        long amountInCents = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        String signature = sha256(reference + amountInCents + currency + properties.integritySecret());
        String redirectUrl = mobile
                ? properties.mobileDeepLink()
                : properties.frontendUrl() == null || properties.frontendUrl().isBlank()
                    ? null
                    : properties.frontendUrl().replaceAll("/$", "") + "/app/tecnico/saldo";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(URI.create(properties.checkoutBaseUrl()))
                .queryParam("public-key", properties.publicKey())
                .queryParam("currency", currency)
                .queryParam("amount-in-cents", amountInCents)
                .queryParam("reference", reference)
                .queryParam("signature:integrity", signature);
        if (redirectUrl != null) builder.queryParam("redirect-url", redirectUrl);
        return builder.build().toUriString();
    }

    public void verifyWebhook(JsonNode body, String headerChecksum) {
        if (!properties.configuredForWebhooks()) {
            throw new ForbiddenException("Wompi events secret is not configured");
        }
        JsonNode signature = body.path("signature");
        JsonNode propertiesNode = signature.path("properties");
        if (!propertiesNode.isArray()) throw new ForbiddenException("Invalid Wompi signature");
        StringBuilder plain = new StringBuilder();
        for (JsonNode property : propertiesNode) {
            plain.append(readPath(body.path("data"), property.asText()));
        }
        plain.append(body.path("timestamp").asText());
        plain.append(properties.eventsSecret());
        String expected = sha256(plain.toString());
        String bodyChecksum = signature.path("checksum").asText();
        boolean matchesBody = expected.equalsIgnoreCase(bodyChecksum);
        boolean matchesHeader = headerChecksum != null && expected.equalsIgnoreCase(headerChecksum);
        if (!matchesBody && !matchesHeader) {
            throw new ForbiddenException("Invalid Wompi event checksum");
        }
    }

    public WompiTransactionSnapshot transaction(String transactionId) {
        if (!properties.configuredForQueries()) {
            throw new IllegalStateException("Wompi public key is not configured");
        }
        JsonNode response = restClient.get()
                .uri("/transactions/{transactionId}", transactionId)
                .header("Authorization", "Bearer " + properties.publicKey())
                .retrieve()
                .body(JsonNode.class);
        JsonNode data = response == null ? null : response.path("data");
        if (data == null || data.isMissingNode() || data.isNull()) {
            throw new IllegalStateException("Wompi transaction response is empty");
        }
        return snapshot(data);
    }

    public WompiTransactionSnapshot snapshot(JsonNode transaction) {
        long amountInCents = transaction.path("amount_in_cents").asLong(-1);
        BigDecimal amount = amountInCents < 0
                ? null
                : BigDecimal.valueOf(amountInCents, 2);
        return new WompiTransactionSnapshot(
                transaction.path("id").asText(null),
                transaction.path("reference").asText(null),
                transaction.path("status").asText(""),
                amount,
                transaction.path("currency").asText(null));
    }

    public String webhookEventKey(JsonNode body, String headerChecksum) {
        if (headerChecksum != null && !headerChecksum.isBlank()) {
            return "WOMPI-" + headerChecksum.trim().toLowerCase();
        }
        return "WOMPI-" + sha256(body.toString());
    }

    private String readPath(JsonNode root, String path) {
        JsonNode current = root;
        for (String part : path.split("\\.")) {
            current = current.path(part);
        }
        if (current.isNumber() || current.isBoolean()) return current.asText();
        return current.isMissingNode() || current.isNull() ? "" : current.asText();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate SHA-256 signature", exception);
        }
    }
}
