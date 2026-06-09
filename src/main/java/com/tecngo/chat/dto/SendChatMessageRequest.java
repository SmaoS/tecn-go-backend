package com.tecngo.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequest(@NotBlank @Size(max = 2000) String message) {}
