package dev.toolkitmc.guiapi.advancement;

import net.minecraft.util.Identifier;

/**
 * Immutable mapping of one advancement → one GUI.
 *
 * @param advancementId  The advancement that triggers the GUI.
 * @param guiId          The GUI to open.
 * @param delayTicks     Ticks to wait before opening (0 = immediate).
 * @param once           If true, only fires on the first grant ever
 *                       (tracked with scoreboard tag "guiadv.<advancementId_sanitized>").
 */
public record AdvancementGuiTrigger(
        Identifier advancementId,
        Identifier guiId,
        int        delayTicks,
        boolean    once
) {
    /**
     * Scoreboard tag name used to mark "already shown" for once=true triggers.
     * Tag: guiadv.<namespace>.<path_with_dots_instead_of_slashes>
     */
    public String onceTag() {
        return "guiadv." + advancementId.getNamespace() + "."
                + advancementId.getPath().replace('/', '.');
    }
}
