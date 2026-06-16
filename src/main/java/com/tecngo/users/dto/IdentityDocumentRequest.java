package com.tecngo.users.dto;

import com.tecngo.users.entity.DocumentType;
import jakarta.validation.constraints.NotNull;

public record IdentityDocumentRequest(
        @NotNull DocumentType documentType,
        String documentFrontUrl,
        String documentBackUrl,
        String documentSingleUrl
) {}
