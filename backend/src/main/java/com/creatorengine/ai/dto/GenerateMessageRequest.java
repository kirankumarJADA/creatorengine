package com.creatorengine.ai.dto;

import com.creatorengine.ai.MessageTone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Inputs the user fills in the "Generate with AI" modal.
 *
 * <p>None of these are persisted — they're just hints to the provider.
 * The {@link #cta} field is optional because not every DM has one
 * (sometimes the goal IS the call to action).</p>
 */
public record GenerateMessageRequest(
        @NotBlank(message = "Goal is required")
        @Size(max = 500, message = "Goal is too long (max 500 characters)")
        String goal,

        @NotNull(message = "Tone is required")
        MessageTone tone,

        @NotBlank(message = "Audience is required")
        @Size(max = 200, message = "Audience is too long (max 200 characters)")
        String audience,

        @Size(max = 200, message = "Call to action is too long (max 200 characters)")
        String cta
) {
    /** Convenience for prompt building — returns "(none)" when empty. */
    public String ctaOrNone() {
        return (cta == null || cta.isBlank()) ? "(none)" : cta.trim();
    }
}
