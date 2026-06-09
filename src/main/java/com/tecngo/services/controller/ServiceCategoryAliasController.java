package com.tecngo.services.controller;

import com.tecngo.services.dto.ServiceCategoryResponse;
import com.tecngo.services.service.ServiceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryAliasController {
    private final ServiceCategoryService service;

    @GetMapping
    public List<ServiceCategoryResponse> active() {
        return service.active();
    }
}
