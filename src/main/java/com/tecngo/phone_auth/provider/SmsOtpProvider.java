package com.tecngo.phone_auth.provider;

public interface SmsOtpProvider {
    String name();
    OtpDispatch send(String phone, int codeLength);
    boolean verify(String phone, String code, String providerReference, String codeHash);
}
