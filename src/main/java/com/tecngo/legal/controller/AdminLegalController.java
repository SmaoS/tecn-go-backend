package com.tecngo.legal.controller;
import com.tecngo.legal.dto.*;
import com.tecngo.legal.service.LegalService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/v1/admin/legal-documents") @RequiredArgsConstructor
public class AdminLegalController {
    private final LegalService service;
    @GetMapping public List<LegalDocumentResponse> list(@AuthenticationPrincipal User user) { return service.adminList(user); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public LegalDocumentResponse create(
            @Valid @RequestBody LegalDocumentRequest request, @AuthenticationPrincipal User user) { return service.create(request, user); }
    @PutMapping("/{id}") public LegalDocumentResponse update(@PathVariable UUID id,
            @Valid @RequestBody LegalDocumentRequest request, @AuthenticationPrincipal User user) { return service.update(id, request, user); }
    @PutMapping("/{id}/activate") public LegalDocumentResponse activate(@PathVariable UUID id,
            @AuthenticationPrincipal User user) { return service.activate(id, user); }
}
