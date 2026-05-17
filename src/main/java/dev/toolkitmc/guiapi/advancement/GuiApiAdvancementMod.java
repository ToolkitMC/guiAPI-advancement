package dev.toolkitmc.guiapi.advancement;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.advancement.criterion.v0.CriterionRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiApiAdvancementMod implements ModInitializer {

    public static final String MOD_ID = "guiapi-advancement";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[GuiAPI-Advancement] Initializing...");

        // Register our custom advancement criterion so datapacks can reference it
        CriterionRegistry.register(OpenGuiCriterion.INSTANCE);

        // Register the trigger-mapping loader (reads data/<ns>/advancement/*.json)
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(AdvancementGuiTriggerLoader.INSTANCE);

        // Hook into Fabric's advancement grant event
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server ->
                AdvancementGrantListener.register(server));

        LOGGER.info("[GuiAPI-Advancement] Ready.");
    }
}
