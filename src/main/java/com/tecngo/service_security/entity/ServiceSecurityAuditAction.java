package com.tecngo.service_security.entity;

public enum ServiceSecurityAuditAction {
    SECURITY_CODE_GENERATED,
    SECURITY_CODE_REGENERATED,
    SECURITY_CODE_VERIFIED,
    SECURITY_CODE_FAILED,
    SECURITY_CODE_LOCKED,
    SECURITY_CODE_CANCELLED
}
