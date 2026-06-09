package com.tecngo.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id, UUID senderId, String senderName, String message, Instant createdAt, Instant readAt
) {}
