package com.tecngo.compliance.config;

import com.tecngo.compliance.entity.AuditOutcome;
import com.tecngo.compliance.service.ComplianceAuditService;
import com.tecngo.observability.CorrelationIdFilter;
import com.tecngo.users.entity.User;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class ComplianceAccessAuditInterceptor implements HandlerInterceptor {
    private final ComplianceAuditService audits;

    @Value("${app.compliance.audit-hmac-secret:${app.jwt.secret}}")
    private String auditSecret;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if (!sensitive(request.getRequestURI())) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User actor = authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
        if (actor == null) return;
        AuditOutcome outcome = exception != null || response.getStatus() >= 500
                ? AuditOutcome.FAILED
                : response.getStatus() >= 400 ? AuditOutcome.DENIED : AuditOutcome.SUCCESS;
        try {
            audits.record(actor, selfSubject(request, actor), resourceType(request.getRequestURI()),
                    resourceId(request.getRequestURI()), request.getMethod() + " " + request.getRequestURI(),
                    outcome, string(request.getAttribute(CorrelationIdFilter.MDC_KEY)),
                    hash(clientIp(request)), null);
        } catch (RuntimeException ignored) {
            // Audit persistence must not replace the original HTTP result.
        }
    }

    private boolean sensitive(String path) {
        return path.startsWith("/api/v1/admin/")
                || path.startsWith("/v1/admin/")
                || path.contains("/v1/files/")
                || path.contains("/v1/verifications/")
                || path.contains("/v1/users/me/data-")
                || path.contains("/v1/compliance/");
    }

    private User selfSubject(HttpServletRequest request, User actor) {
        return request.getRequestURI().contains("/users/me/") ? actor : null;
    }

    private String resourceType(String path) {
        if (path.contains("/files/")) return "PRIVATE_FILE";
        if (path.contains("/verifications/")) return "IDENTITY_VERIFICATION";
        if (path.contains("/incidents")) return "COMPLIANCE_INCIDENT";
        if (path.contains("/data-requests") || path.contains("/data-export")) return "PERSONAL_DATA";
        return "ADMIN_RESOURCE";
    }

    private String resourceId(String path) {
        String[] parts = path.split("/");
        return parts.length == 0 ? null : parts[parts.length - 1];
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(auditSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            return null;
        }
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}
