package com.creatorengine.automation.dto;

import com.creatorengine.automation.entity.Automation;
import jakarta.validation.constraints.Size;

public record PublicReplyDto(
        @Size(max = 300, message = "Public reply is too long (max 300 characters)")
        String text,
        Boolean enabled
) {
    public Automation.PublicReply toEntity() {
        return new Automation.PublicReply(
                text == null ? null : text.trim(),
                enabled == null || enabled
        );
    }

    public static PublicReplyDto from(Automation.PublicReply r) {
        if (r == null) {
            return null;
        }
        return new PublicReplyDto(r.getText(), r.getEnabled());
    }
}