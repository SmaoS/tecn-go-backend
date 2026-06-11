package com.tecngo.legal.dto;
import com.tecngo.legal.entity.LegalRoleTarget;
import jakarta.validation.constraints.*;
public record LegalDocumentRequest(@NotBlank String code, @NotBlank String title, @NotBlank String version,
        @NotNull LegalRoleTarget roleTarget, @NotBlank String content, boolean active) {}
