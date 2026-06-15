package com.tecngo.content_moderation.dto;

import jakarta.validation.constraints.Size;

public record ModerationDecisionRequest(@Size(max = 1000) String reason) {}
