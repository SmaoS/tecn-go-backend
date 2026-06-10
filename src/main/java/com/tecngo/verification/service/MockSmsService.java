package com.tecngo.verification.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
public class MockSmsService implements SmsService {
    @Override public void sendOtp(String phone) {}
    @Override public boolean verifyOtp(String phone, String code) { return "000000".equals(code); }
}
