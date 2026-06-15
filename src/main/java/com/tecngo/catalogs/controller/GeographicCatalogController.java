package com.tecngo.catalogs.controller;

import com.tecngo.catalogs.dto.CatalogItemResponse;
import com.tecngo.catalogs.service.GeographicCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/catalogs")
@RequiredArgsConstructor
public class GeographicCatalogController {
    private final GeographicCatalogService service;

    @GetMapping("/countries")
    public List<CatalogItemResponse> countries() {
        return service.countries();
    }

    @GetMapping("/departments")
    public List<CatalogItemResponse> departments(@RequestParam UUID countryId) {
        return service.departments(countryId);
    }

    @GetMapping("/cities")
    public List<CatalogItemResponse> cities(@RequestParam UUID departmentId) {
        return service.cities(departmentId);
    }
}
