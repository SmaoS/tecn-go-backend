package com.tecngo.wompi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tecngo.shared.exception.ForbiddenException;
import com.tecngo.wompi.config.WompiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class WompiPaymentService {
    private final WompiProperties properties;

    public String checkoutUrl(String reference, BigDecimal amount, String currency) {
        if (!properties.configuredForCheckout()) {
            throw new IllegalStateException("Wompi checkout keys are not configured");
        }
        long amountInCents = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        String signature = sha256(reference + amountInCents + currency + properties.integritySecret());
        String redirectUrl = properties.frontendUrl() == null || properties.frontendUrl().isBlank()
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
