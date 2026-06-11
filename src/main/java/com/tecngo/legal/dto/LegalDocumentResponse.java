package com.tecngo.legal.dto;
import com.tecngo.legal.entity.LegalRoleTarget;
import java.time.Instant;
import java.util.UUID;
public record LegalDocumentResponse(UUID id, String code, String title, String version,
        LegalRoleTarget roleTarget, String content, boolean active, boolean accepted, Instant createdAt) {}
