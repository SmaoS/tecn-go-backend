package com.tecngo.shared.exception;

public class TooManyRequestsException extends RuntimeException {
    private final String code;

    public TooManyRequestsException(String message) {
        this("RATE_LIMITED", message);
    }

    public TooManyRequestsException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
