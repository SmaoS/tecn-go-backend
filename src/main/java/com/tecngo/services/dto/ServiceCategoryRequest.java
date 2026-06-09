package com.tecngo.services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceCategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        boolean active
) {}
