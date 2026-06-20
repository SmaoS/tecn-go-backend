package com.tecngo.phone_auth.dto;

import java.time.Instant;

public record SendPhoneOtpResponse(String message, Instant expiresAt, String debugCode) {
}
