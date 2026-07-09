package com.tecngo.verification.service;

public interface EmailSender {
    void sendVerification(String recipient, String recipientName, String verificationUrl);

    void sendPasswordReset(String recipient, String recipientName, String resetUrl);

    void sendMfaCode(String recipient, String recipientName, String code, long expirationMinutes);

    void sendDataExport(String recipient, String recipientName, String fileName, byte[] content);

    void sendTechnicianProfileApproved(String recipient, String recipientName);
}
