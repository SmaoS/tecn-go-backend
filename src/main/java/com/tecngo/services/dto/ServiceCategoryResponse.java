package com.tecngo.services.dto;

import java.util.UUID;

public record ServiceCategoryResponse(UUID id, String name, String slug, String description, boolean active) {}
