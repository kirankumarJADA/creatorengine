package com.creatorengine.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 80, message = "Name is too long.")
        String name
) {
}