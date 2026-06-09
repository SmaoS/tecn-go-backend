package com.tecngo.chat.controller;

import com.tecngo.chat.dto.ChatMessageResponse;
import com.tecngo.chat.dto.SendChatMessageRequest;
import com.tecngo.chat.service.ChatService;
import com.tecngo.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/service-requests/{requestId}/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService service;

    @GetMapping
    public List<ChatMessageResponse> messages(@PathVariable UUID requestId,
                                              @AuthenticationPrincipal User user) {
        return service.messages(requestId, user);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse send(@PathVariable UUID requestId,
                                    @Valid @RequestBody SendChatMessageRequest request,
                                    @AuthenticationPrincipal User user) {
        return service.send(requestId, request.message(), user);
    }

    @PutMapping("/read")
    public Map<String, Integer> markRead(@PathVariable UUID requestId,
                                         @AuthenticationPrincipal User user) {
        return Map.of("updated", service.markRead(requestId, user));
    }
}
