package com.tecngo.legal.controller;
import com.tecngo.legal.dto.*;
import com.tecngo.legal.service.LegalService;
import com.tecngo.users.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/v1") @RequiredArgsConstructor
public class LegalController {
    private final LegalService service;
    @GetMapping("/legal/documents/active") public List<LegalDocumentResponse> active(@AuthenticationPrincipal User user) { return service.active(user); }
    @GetMapping("/legal/documents/public") public List<LegalDocumentResponse> publicActive() { return service.publicActive(); }
    @PostMapping("/legal/documents/{id}/accept") public LegalDocumentResponse accept(@PathVariable UUID id,
            @AuthenticationPrincipal User user, HttpServletRequest request) { return service.accept(id, user, request); }
    @GetMapping("/users/me/legal-status") public LegalStatusResponse status(@AuthenticationPrincipal User user) { return service.status(user); }
}
