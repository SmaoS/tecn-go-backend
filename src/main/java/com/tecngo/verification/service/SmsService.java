package com.tecngo.verification.service;

public interface SmsService {
    void sendOtp(String phone);
    boolean verifyOtp(String phone, String code);
}
