package com.tecngo.phone_auth.service;

import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer {
    public String normalize(String rawPhone) {
        if (rawPhone == null) throw new IllegalArgumentException("Phone is required");
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.length() == 10 && digits.startsWith("3")) digits = "57" + digits;
        if (digits.length() < 10 || digits.length() > 15) {
            throw new IllegalArgumentException("Enter a valid phone number including country code");
        }
        return "+" + digits;
    }
}
