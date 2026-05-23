package com.creatorengine.automation.dto;

import jakarta.validation.constraints.NotNull;

/** Body for {@code PATCH /api/automations/{id}/toggle}. */
public record ToggleRequest(
        @NotNull(message = "enabled is required")
        Boolean enabled
) {}
