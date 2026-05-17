package dev.toolkitmc.guiapi.advancement.mixin;

import dev.toolkitmc.guiapi.advancement.AdvancementGuiTrigger;
import dev.toolkitmc.guiapi.advancement.AdvancementGuiTriggerLoader;
import dev.toolkitmc.guiapi.advancement.GuiApiAdvancementMod;
import dev.toolkitmc.guiapi.gui.BarrelGuiHandler;
import dev.toolkitmc.guiapi.gui.GuiDefinition;
import dev.toolkitmc.guiapi.loader.GuiRegistry;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Shadow
    public abstract AdvancementProgress getProgress(AdvancementEntry advancement);

    @Inject(
        method = "grantCriterion",
        at = @At("RETURN")
    )
    private void guiapi$onCriterionGranted(AdvancementEntry advancement, String criterionName,
                                            CallbackInfoReturnable<Boolean> cir) {
        // Only act when a criterion was actually newly granted (return value = true)
        if (!cir.getReturnValue()) return;

        // Only act when the entire advancement is now complete
        if (!getProgress(advancement).isDone()) return;

        ServerPlayerEntity player = this.owner;
        if (player == null) return;

        Identifier advId = advancement.id();
        AdvancementGuiTrigger trigger = AdvancementGuiTriggerLoader.INSTANCE.getTrigger(advId);
        if (trigger == null) return;

        // once=true: skip if already shown
        if (trigger.once() && player.getCommandTags().contains(trigger.onceTag())) return;

        GuiRegistry.INSTANCE.get(trigger.guiId()).ifPresentOrElse(
            gui -> {
                if (trigger.once()) player.addCommandTag(trigger.onceTag());

                if (trigger.delayTicks() <= 0) {
                    BarrelGuiHandler.open(player, gui);
                } else {
                    scheduleDelayed(player, gui, trigger.delayTicks());
                }
            },
            () -> GuiApiAdvancementMod.LOGGER.warn(
                "[GuiAPI-Advancement] GUI not found: {} (triggered by advancement {})",
                trigger.guiId(), advId)
        );
    }

    private void scheduleDelayed(ServerPlayerEntity player, GuiDefinition gui, int delayTicks) {
        if (player.getServer() == null) return;
        long targetTick = player.getServer().getTicks() + delayTicks;

        // Use a one-shot flag to avoid reopening every tick after targetTick
        boolean[] fired = {false};
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!fired[0] && server.getTicks() >= targetTick && player.isAlive()) {
                fired[0] = true;
                BarrelGuiHandler.open(player, gui);
            }
        });
    }
}
