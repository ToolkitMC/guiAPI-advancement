package dev.toolkitmc.guiapi.advancement;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads trigger mappings from data/<ns>/advancement/*.json.
 *
 * File format:
 * {
 *   "advancement": "minecraft:story/obtain_armor",
 *   "gui":         "mypack:welcome_gui",
 *   "delay_ticks": 20,
 *   "once":        true
 * }
 *
 * Fields:
 *   advancement  — Advancement identifier to listen for (required)
 *   gui          — GUI identifier to open when the advancement is granted (required)
 *   delay_ticks  — Ticks to wait before opening the GUI (default: 0)
 *   once         — If true, GUI is only opened the first time the advancement
 *                  is granted (tracked via a scoreboard tag). Default: false.
 */
public class AdvancementGuiTriggerLoader
        extends SinglePreparationResourceReloader<Map<Identifier, AdvancementGuiTrigger>>
        implements IdentifiableResourceReloadListener {

    public static final AdvancementGuiTriggerLoader INSTANCE = new AdvancementGuiTriggerLoader();

    private static final String DIRECTORY = "advancement";
    private static final Gson   GSON      = new Gson();

    /** advancement id → trigger */
    private final Map<Identifier, AdvancementGuiTrigger> triggers = new HashMap<>();

    private AdvancementGuiTriggerLoader() {}

    @Override
    public Identifier getFabricId() {
        return Identifier.of("guiapi-advancement", "trigger_loader");
    }

    @Override
    protected Map<Identifier, AdvancementGuiTrigger> prepare(ResourceManager manager, Profiler profiler) {
        Map<Identifier, AdvancementGuiTrigger> loaded = new HashMap<>();

        manager.findResources(DIRECTORY, id -> id.getPath().endsWith(".json"))
               .forEach((fileId, resource) -> {
                   try (InputStreamReader reader = new InputStreamReader(
                           resource.getInputStream(), StandardCharsets.UTF_8)) {

                       JsonObject json = GSON.fromJson(reader, JsonObject.class);
                       if (json == null) return;

                       if (!json.has("advancement") || !json.has("gui")) {
                           GuiApiAdvancementMod.LOGGER.warn(
                                   "[GuiAPI-Advancement] Skipping {} — missing 'advancement' or 'gui' field.", fileId);
                           return;
                       }

                       Identifier advId = Identifier.tryParse(json.get("advancement").getAsString());
                       Identifier guiId = Identifier.tryParse(json.get("gui").getAsString());

                       if (advId == null || guiId == null) {
                           GuiApiAdvancementMod.LOGGER.warn(
                                   "[GuiAPI-Advancement] Skipping {} — invalid identifier.", fileId);
                           return;
                       }

                       int  delayTicks = json.has("delay_ticks") ? json.get("delay_ticks").getAsInt() : 0;
                       boolean once    = json.has("once") && json.get("once").getAsBoolean();

                       AdvancementGuiTrigger trigger = new AdvancementGuiTrigger(advId, guiId, delayTicks, once);
                       loaded.put(advId, trigger);

                       GuiApiAdvancementMod.LOGGER.info(
                               "[GuiAPI-Advancement] Loaded trigger: {} → {}", advId, guiId);

                   } catch (Exception e) {
                       GuiApiAdvancementMod.LOGGER.error(
                               "[GuiAPI-Advancement] Failed to load {}: {}", fileId, e.getMessage());
                   }
               });

        return loaded;
    }

    @Override
    protected void apply(Map<Identifier, AdvancementGuiTrigger> prepared,
                         ResourceManager manager, Profiler profiler) {
        triggers.clear();
        triggers.putAll(prepared);
        GuiApiAdvancementMod.LOGGER.info(
                "[GuiAPI-Advancement] Registered {} advancement trigger(s).", triggers.size());
    }

    public Map<Identifier, AdvancementGuiTrigger> getTriggers() {
        return Map.copyOf(triggers);
    }

    public AdvancementGuiTrigger getTrigger(Identifier advancementId) {
        return triggers.get(advancementId);
    }
}
