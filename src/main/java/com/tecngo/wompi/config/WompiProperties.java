package com.tecngo.wompi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wompi")
public record WompiProperties(
        String publicKey,
        String privateKey,
        String eventsSecret,
        String integritySecret,
        String env,
        String frontendUrl,
        String mobileDeepLink
) {
    public String checkoutBaseUrl() {
        return "https://checkout.wompi.co/p/";
    }

    public boolean configuredForCheckout() {
        return publicKey != null && !publicKey.isBlank()
                && integritySecret != null && !integritySecret.isBlank();
    }

    public boolean configuredForWebhooks() {
        return eventsSecret != null && !eventsSecret.isBlank();
    }

    public boolean configuredForQueries() {
        return publicKey != null && !publicKey.isBlank();
    }

    public String apiBaseUrl() {
        return "prod".equalsIgnoreCase(env) || "production".equalsIgnoreCase(env)
                ? "https://production.wompi.co/v1"
                : "https://sandbox.wompi.co/v1";
    }
}
