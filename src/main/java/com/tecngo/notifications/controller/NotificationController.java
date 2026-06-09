package com.tecngo.notifications.controller;

import com.tecngo.notifications.dto.NotificationResponse;
import com.tecngo.notifications.service.NotificationService;
import com.tecngo.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public List<NotificationResponse> mine(@AuthenticationPrincipal User user) {
        return service.mine(user);
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return service.markRead(id, user);
    }
}
