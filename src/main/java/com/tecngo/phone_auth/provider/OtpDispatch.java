package com.tecngo.phone_auth.provider;

public record OtpDispatch(String providerReference, String codeForHash, String debugCode) {
}
