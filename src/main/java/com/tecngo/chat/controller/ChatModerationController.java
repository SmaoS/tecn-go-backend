package com.tecngo.chat.controller;

import com.tecngo.chat.dto.*;
import com.tecngo.chat.entity.ChatModerationStatus;
import com.tecngo.chat.service.ChatReviewService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatModerationController {
    private final ChatReviewService service;

    @PostMapping({"/chat/messages/{id}/report", "/v1/chat/messages/{id}/report"})
    public ChatMessageResponse report(@PathVariable UUID id,
                                      @Valid @RequestBody ReportChatMessageRequest request,
                                      @AuthenticationPrincipal User user) {
        return service.report(id, request.reason(), user);
    }

    @GetMapping("/v1/admin/chat-moderation/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
    public List<AdminChatMessageResponse> queue(
            @RequestParam(required = false) ChatModerationStatus status,
            @AuthenticationPrincipal User user) {
        return service.queue(status, user);
    }

    @PutMapping("/v1/admin/chat-moderation/messages/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
    public AdminChatMessageResponse approve(@PathVariable UUID id,
                                             @RequestBody(required = false) ReviewChatMessageRequest request,
                                             @AuthenticationPrincipal User user) {
        return service.decide(id, ChatModerationStatus.APPROVED,
                request == null ? null : request.reason(), user);
    }

    @PutMapping("/v1/admin/chat-moderation/messages/{id}/block")
    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIER')")
    public AdminChatMessageResponse block(@PathVariable UUID id,
                                           @Valid @RequestBody ReviewChatMessageRequest request,
                                           @AuthenticationPrincipal User user) {
        return service.decide(id, ChatModerationStatus.BLOCKED, request.reason(), user);
    }

    @PutMapping("/v1/admin/chat-moderation/messages/{id}/sanction")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminChatMessageResponse sanction(@PathVariable UUID id,
                                              @Valid @RequestBody ReviewChatMessageRequest request,
                                              @AuthenticationPrincipal User user) {
        return service.sanction(id, request.reason(), user);
    }
}
