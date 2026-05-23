package com.creatorengine.ai;

/**
 * Style register the AI should match in the generated DM.
 *
 * <p>Kept here rather than {@code automation/entity} because it's
 * purely a generation-time hint — it doesn't get persisted with the
 * automation. The user picks one in the modal and the suggestions
 * come back targeting that vibe; the chosen message is then stored
 * as plain text.</p>
 */
public enum MessageTone {
    FRIENDLY,
    PROFESSIONAL,
    SALES,
    CASUAL
}
