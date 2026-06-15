package com.tecngo.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportChatMessageRequest(@NotBlank @Size(max = 1000) String reason) {
}
