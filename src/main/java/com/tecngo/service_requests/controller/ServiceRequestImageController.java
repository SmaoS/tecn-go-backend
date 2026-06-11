package com.tecngo.service_requests.controller;

import com.tecngo.service_requests.dto.ServiceRequestImageResponse;
import com.tecngo.service_requests.service.ServiceRequestImageService;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/service-requests/{requestId}/images")
@RequiredArgsConstructor
public class ServiceRequestImageController {
    private final ServiceRequestImageService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestImageResponse upload(@PathVariable UUID requestId,
                                              @RequestPart("file") MultipartFile file,
                                              @AuthenticationPrincipal User user) {
        return service.upload(requestId, file, user);
    }

    @GetMapping
    public List<ServiceRequestImageResponse> list(@PathVariable UUID requestId,
                                                  @AuthenticationPrincipal User user) {
        return service.list(requestId, user);
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID requestId, @PathVariable UUID imageId,
                       @AuthenticationPrincipal User user) {
        service.delete(requestId, imageId, user);
    }
}
