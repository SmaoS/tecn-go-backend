package com.tecngo.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProfileSelfieChangeRequest(
        @NotBlank @Size(max = 1000) String reason
) {}
