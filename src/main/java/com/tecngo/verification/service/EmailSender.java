package com.tecngo.verification.service;

public interface EmailSender {
    void sendVerification(String recipient, String recipientName, String verificationUrl);
}
