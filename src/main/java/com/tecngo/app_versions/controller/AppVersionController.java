package com.tecngo.app_versions.controller;
import com.tecngo.app_versions.dto.*;
import com.tecngo.app_versions.entity.AppPlatform;
import com.tecngo.app_versions.service.AppVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequiredArgsConstructor
public class AppVersionController {
    private final AppVersionService service;
    @GetMapping("/v1/app-version/check")
    public AppVersionCheckResponse check(@RequestParam AppPlatform platform,@RequestParam String currentVersion){return service.check(platform,currentVersion);}
    @GetMapping("/v1/admin/app-versions") @PreAuthorize("hasRole('ADMIN')")
    public List<AppVersionResponse> list(){return service.list();}
    @PutMapping("/v1/admin/app-versions/{platform}") @PreAuthorize("hasRole('ADMIN')")
    public AppVersionResponse update(@PathVariable AppPlatform platform,@Valid @RequestBody UpdateAppVersionRequest request){return service.update(platform,request);}
}
