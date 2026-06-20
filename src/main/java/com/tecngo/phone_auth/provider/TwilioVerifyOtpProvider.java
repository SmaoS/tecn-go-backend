package com.tecngo.phone_auth.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
public class TwilioVerifyOtpProvider implements SmsOtpProvider {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String serviceSid;

    public TwilioVerifyOtpProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.sms.twilio.account-sid}") String accountSid,
            @Value("${app.sms.twilio.auth-token}") String authToken,
            @Value("${app.sms.twilio.verify-service-sid}") String serviceSid) {
        this.client = builder
                .baseUrl("https://verify.twilio.com/v2/Services/" + serviceSid)
                .defaultHeaders(headers -> headers.setBasicAuth(accountSid, authToken))
                .build();
        this.objectMapper = objectMapper;
        this.serviceSid = serviceSid;
    }

    @Override
    public String name() {
        return "twilio";
    }

    @Override
    public OtpDispatch send(String phone, int codeLength) {
        var body = new LinkedMultiValueMap<String, String>();
        body.add("To", phone);
        body.add("Channel", "sms");
        String response = client.post()
                .uri("/Verifications")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(String.class);
        return new OtpDispatch(readSid(response), null, null);
    }

    @Override
    public boolean verify(String phone, String code, String providerReference, String codeHash) {
        var body = new LinkedMultiValueMap<String, String>();
        body.add("To", phone);
        body.add("Code", code);
        String response = client.post()
                .uri("/VerificationCheck")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            JsonNode json = objectMapper.readTree(response);
            return "approved".equalsIgnoreCase(json.path("status").asText());
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid Twilio Verify response", exception);
        }
    }

    private String readSid(String response) {
        try {
            return objectMapper.readTree(response).path("sid").asText(serviceSid);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid Twilio Verify response", exception);
        }
    }
}
