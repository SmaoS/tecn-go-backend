package com.tecngo.users.dto;

import com.tecngo.users.entity.DocumentType;
import com.tecngo.users.entity.IdentityDocumentCaptureStatus;
import jakarta.validation.constraints.NotNull;

public record IdentityDocumentRequest(
        @NotNull DocumentType documentType,
        String documentFrontUrl,
        String documentBackUrl,
        String documentSingleUrl,
        IdentityDocumentCaptureStatus identityDocumentCaptureStatus
) {}
