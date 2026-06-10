package com.tecngo.shared.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class VersionController {
    @GetMapping("/version")
    public Map<String, String> version(
            @Value("${info.app.name:TecnGo}") String name,
            @Value("${info.app.version:1.0.0}") String version,
            @Value("${info.app.environment:unknown}") String environment) {
        return Map.of("name", name, "version", version, "environment", environment);
    }
}
