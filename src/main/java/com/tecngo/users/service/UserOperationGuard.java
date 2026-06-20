package com.tecngo.users.service;

import com.tecngo.shared.exception.CodedForbiddenException;
import com.tecngo.users.entity.Role;
import com.tecngo.users.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserOperationGuard {
    @Value("${app.email.require-verification:false}")
    private boolean requireEmailVerification;
    @Value("${app.onboarding.require-completion:false}")
    private boolean requireOnboardingCompletion;

    public void requireAllowed(User user, String method, String path) {
        if (user == null || user.getRole() == Role.ADMIN || user.getRole() == Role.VERIFIER) return;
        if (isAlwaysAllowed(method, path)) return;
        if (requireEmailVerification && !user.isEmailVerified() && !user.isPhoneVerified()) {
            throw new CodedForbiddenException("CONTACT_NOT_VERIFIED",
                    "Debes confirmar tu correo electrónico o celular para continuar.");
        }
        if (isOnboardingAllowed(method, path)) return;
        if (requireOnboardingCompletion && !user.isOnboardingCompleted()) {
            if (user.getRole() == Role.TECHNICIAN) {
                throw new CodedForbiddenException("TECHNICIAN_PROFILE_INCOMPLETE",
                        "Completa tu perfil técnico para poder operar.");
            }
            throw new CodedForbiddenException("ONBOARDING_REQUIRED",
                    "Debes completar tu inscripción para continuar.");
        }
    }

    private boolean isAlwaysAllowed(String method, String path) {
        return path.equals("/v1/auth/send-email-verification")
                || path.equals("/v1/users/me/profile")
                || path.equals("/v1/users/me/onboarding-status")
                || path.startsWith("/v1/legal/")
                || path.equals("/v1/users/me/legal-status")
                || path.equals("/v1/auth/verify-email")
                || path.equals("/v1/auth/login")
                || path.equals("/v1/auth/register")
                || path.startsWith("/v1/auth/phone/")
                || path.equals("/v1/auth/register-by-phone")
                || path.equals("/v1/auth/login-by-phone")
                || path.equals("/error")
                || method.equals("OPTIONS")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/");
    }

    private boolean isOnboardingAllowed(String method, String path) {
        return path.startsWith("/v1/users/me/onboarding")
                || path.startsWith("/v1/technicians/me/onboarding")
                || path.startsWith("/v1/catalogs/")
                || path.equals("/v1/service-categories")
                || path.equals("/v1/services")
                || path.equals("/v1/files/upload")
                || path.equals("/v1/users/me/fcm-token");
    }
}
