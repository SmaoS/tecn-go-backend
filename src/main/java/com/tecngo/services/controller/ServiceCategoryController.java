package com.tecngo.services.controller;

import com.tecngo.services.dto.ServiceCategoryResponse;
import com.tecngo.services.service.ServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/services")
@RequiredArgsConstructor
public class ServiceCategoryController {
    private final ServiceCategoryService service;

    @GetMapping
    public List<ServiceCategoryResponse> findAll() {
        return service.active();
    }
}
