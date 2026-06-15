package com.tecngo.verification.service;

public interface EmailSender {
    void sendVerification(String recipient, String recipientName, String verificationUrl);

    void sendPasswordReset(String recipient, String recipientName, String resetUrl);
}
