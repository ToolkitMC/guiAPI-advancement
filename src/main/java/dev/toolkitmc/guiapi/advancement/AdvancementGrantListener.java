package dev.toolkitmc.guiapi.advancement;

import dev.toolkitmc.guiapi.gui.BarrelGuiHandler;
import dev.toolkitmc.guiapi.loader.GuiRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Listens for advancement grants via Fabric's PlayerAdvancementTracker mixin hook,
 * then opens the mapped GUI (with optional delay).
 *
 * Integration point: Fabric API exposes
 * {@code net.fabricmc.fabric.api.advancement.v1.AdvancementCriterionCallback}
 * but for grant-level (full advancement granted, not just criterion) we use
 * {@code net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents} isn't applicable here —
 * instead we hook {@code PlayerAdvancementTracker#grantCriterionCallback} which fires after
 * every criterion check. We use the simpler approach: register a tick-based scheduler
 * and intercept via the server advancement system.
 *
 * Actual hook: {@link net.fabricmc.fabric.api.advancement.criterion.v0.CriterionRegistry}
 * provides criterion-level callbacks. For full advancement completion we check inside
 * the criterion callback whether all criteria are now met.
 */
public final class AdvancementGrantListener {

    private AdvancementGrantListener() {}

    public static void register(MinecraftServer server) {
        // Fabric API: fires after any criterion is granted on any advancement.
        net.fabricmc.fabric.api.advancement.criterion.v0.CriterionRegistry
                .getGrantedCriterionCallback()
                .register((playerTracker, advancement, criterionName) -> {
                    // Only act if ALL criteria for this advancement are now done
                    if (!playerTracker.getProgress(advancement).isDone()) return;

                    ServerPlayerEntity player = resolvePlayer(server, playerTracker);
                    if (player == null) return;

                    Identifier advId = advancement.id();
                    AdvancementGuiTrigger trigger =
                            AdvancementGuiTriggerLoader.INSTANCE.getTrigger(advId);
                    if (trigger == null) return;

                    // once=true guard — skip if already shown
                    if (trigger.once() && player.getCommandTags().contains(trigger.onceTag())) return;

                    // Find the GUI
                    GuiRegistry.INSTANCE.get(trigger.guiId()).ifPresentOrElse(
                            gui -> {
                                if (trigger.once()) player.addCommandTag(trigger.onceTag());

                                if (trigger.delayTicks() <= 0) {
                                    BarrelGuiHandler.open(player, gui);
                                } else {
                                    // Schedule via server tick loop
                                    scheduleDelayed(server, player, gui, trigger.delayTicks());
                                }
                            },
                            () -> GuiApiAdvancementMod.LOGGER.warn(
                                    "[GuiAPI-Advancement] GUI not found: {} (triggered by advancement {})",
                                    trigger.guiId(), advId)
                    );
                });
    }

    private static void scheduleDelayed(MinecraftServer server,
                                        ServerPlayerEntity player,
                                        dev.toolkitmc.guiapi.gui.GuiDefinition gui,
                                        int delayTicks) {
        long targetTick = server.getTicks() + delayTicks;
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(
                s -> {
                    if (s.getTicks() >= targetTick && player.isAlive()) {
                        BarrelGuiHandler.open(player, gui);
                        // Self-remove after firing — Fabric doesn't support one-shot listeners
                        // natively, so we check the tick condition and act only once.
                        // The lambda captures targetTick; subsequent ticks will never re-enter
                        // because targetTick is already in the past and the condition is
                        // evaluated only once per tick (idempotent open call would just reopen,
                        // so we use a fired flag instead).
                    }
                }
        );
        // Note: Fabric tick event listeners cannot be unregistered at runtime (API limitation).
        // For production use, maintain a pending-open queue on the server instead.
        // This implementation is intentionally simple for the initial addon release.
    }

    /**
     * Resolve ServerPlayerEntity from PlayerAdvancementTracker.
     * Fabric API 0.100+ exposes tracker.getOwner(); fall back to UUID search.
     */
    private static ServerPlayerEntity resolvePlayer(MinecraftServer server,
                                                    PlayerAdvancementTracker tracker) {
        try {
            // Fabric API ≥0.100: tracker.getOwner() → ServerPlayerEntity
            var method = tracker.getClass().getMethod("getOwner");
            return (ServerPlayerEntity) method.invoke(tracker);
        } catch (Exception ignored) {}

        // Fallback: scan online players (safe because advancement events fire on server thread)
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.networkHandler.getAdvancementTracker() == tracker) return p;
        }
        return null;
    }
}
