package com.tecngo.catalogs.dto;

import java.util.UUID;

public record CatalogItemResponse(UUID id, String name, String phonePrefix) {
    public CatalogItemResponse(UUID id, String name) {
        this(id, name, null);
    }
}
