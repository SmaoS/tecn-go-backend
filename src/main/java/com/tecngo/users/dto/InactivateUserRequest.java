package com.tecngo.users.dto;

import com.tecngo.users.entity.InactivationReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InactivateUserRequest(
        @NotNull InactivationReason reason,
        @NotBlank @Size(max = 1000) String comment
) {
}
