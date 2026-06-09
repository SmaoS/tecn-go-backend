package com.tecngo.admin.controller;

import com.tecngo.services.dto.ServiceCategoryRequest;
import com.tecngo.services.dto.ServiceCategoryResponse;
import com.tecngo.services.service.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/service-categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceCategoryController {
    private final ServiceCategoryService service;

    @GetMapping
    public List<ServiceCategoryResponse> all() {
        return service.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceCategoryResponse create(@Valid @RequestBody ServiceCategoryRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ServiceCategoryResponse update(@PathVariable UUID id,
                                          @Valid @RequestBody ServiceCategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
