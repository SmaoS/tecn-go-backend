package com.tecngo.verification.controller;

import com.tecngo.users.entity.User;
import com.tecngo.verification.dto.EmailUpdateResponse;
import com.tecngo.verification.dto.UpdateEmailRequest;
import com.tecngo.verification.dto.VerificationMessage;
import com.tecngo.verification.dto.VerifyEmailRequest;
import com.tecngo.verification.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService service;

    @PostMapping({"/send-email-verification", "/resend-email-verification"})
    public VerificationMessage send(@AuthenticationPrincipal User user) {
        service.send(user);
        return new VerificationMessage("Verification email sent", user.isEmailVerified());
    }

    @PutMapping("/email")
    public EmailUpdateResponse updateEmail(@AuthenticationPrincipal User user,
                                           @Valid @RequestBody UpdateEmailRequest request) {
        User updated = service.updateEmailAndSend(user, request.email(), request.confirmEmail());
        return new EmailUpdateResponse("Verification email sent", updated.getEmail(), updated.isEmailVerified());
    }

    @PostMapping("/verify-email")
    public VerificationMessage verify(@Valid @RequestBody VerifyEmailRequest request) {
        User user = service.verify(request.token());
        return new VerificationMessage("Email verified", user.isEmailVerified());
    }
}
