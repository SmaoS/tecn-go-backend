package com.tecngo.phone_auth.dto;

public record VerifyPhoneOtpResponse(boolean verified, String verificationToken) {
}
