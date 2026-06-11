package com.tecngo.system_parameters.controller;

import com.tecngo.system_parameters.dto.SystemParameterResponse;
import com.tecngo.system_parameters.dto.UpdateSystemParameterRequest;
import com.tecngo.system_parameters.service.SystemParameterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/system-parameters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemParameterController {
    private final SystemParameterService service;

    @GetMapping
    public List<SystemParameterResponse> list() {
        return service.list();
    }

    @PutMapping("/{key}")
    public SystemParameterResponse update(@PathVariable String key,
                                          @Valid @RequestBody UpdateSystemParameterRequest request) {
        return service.update(key, request.value());
    }
}
