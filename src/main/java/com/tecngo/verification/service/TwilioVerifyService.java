package com.tecngo.verification.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class TwilioVerifyService implements SmsService {
    @Override
    public void sendOtp(String phone) {
        throw new UnsupportedOperationException("SMS verification is not enabled yet");
    }

    @Override
    public boolean verifyOtp(String phone, String code) {
        throw new UnsupportedOperationException("SMS verification is not enabled yet");
    }
}
