package com.tecngo.phone_auth.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsOtpProvider implements SmsOtpProvider {
    private final PasswordEncoder passwordEncoder;

    @Value("${app.sms.mock-code:00000}")
    private String configuredCode;
    @Value("${app.sms.expose-debug-code:false}")
    private boolean exposeDebugCode;

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public OtpDispatch send(String phone, int codeLength) {
        String code = configuredCode.matches("\\d{" + codeLength + "}")
                ? configuredCode
                : "0".repeat(codeLength);
        return new OtpDispatch(null, passwordEncoder.encode(code), exposeDebugCode ? code : null);
    }

    @Override
    public boolean verify(String phone, String code, String providerReference, String codeHash) {
        return codeHash != null && passwordEncoder.matches(code, codeHash);
    }
}
