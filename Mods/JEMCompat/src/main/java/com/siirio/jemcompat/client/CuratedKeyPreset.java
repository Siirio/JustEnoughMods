package com.siirio.jemcompat.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Explicit, versioned key-only preset. Existing personal options are never a distribution input. */
@Mod.EventBusSubscriber(modid = "jemcompat", value = Dist.CLIENT)
public final class CuratedKeyPreset {
    private static final String REVISION = "2026-10-20";
    private static boolean cleanOptions, initialized;
    private CuratedKeyPreset() { }
    public static void observeOptions(Path file) { if (!Files.exists(file)) cleanOptions = true; }
    private static Path marker() { return Minecraft.getInstance().gameDirectory.toPath().resolve("config/jemcompat-key-preset.applied"); }
    private static Map<String, String> desired() {
        try (var stream = CuratedKeyPreset.class.getResourceAsStream("/assets/jemcompat/input/default_keys.json")) {
            if (stream == null) throw new java.io.IOException("Missing curated key resource");
            return new Gson().fromJson(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8),
                    new TypeToken<LinkedHashMap<String, String>>() { }.getType());
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot read JEM key preset", failure); }
    }
    private static Map<KeyMapping, String> changes() {
        Map<KeyMapping, String> result = new LinkedHashMap<>();
        Map<String, String> desired = desired();
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            String key = desired.get(mapping.getName());
            if (key != null && !key.equals(serialized(mapping))) result.put(mapping, key);
        }
        return result;
    }
    private static String serialized(KeyMapping mapping) {
        return mapping.saveString() + (mapping.getKeyModifier() == KeyModifier.NONE ? "" : ":" + mapping.getKeyModifier().name());
    }
    private static void feedback(CommandSourceStack source, Component message) { source.sendSuccess(() -> message, false); }
    private static int preview(CommandSourceStack source) {
        if (Files.exists(marker())) { feedback(source, Component.translatable("gui.jemcompat.keys.applied")); return 0; }
        var changes = changes();
        feedback(source, Component.translatable("gui.jemcompat.keys.preview", changes.size()));
        changes.forEach((mapping, value) -> feedback(source, Component.translatable(mapping.getName()).append(": " + serialized(mapping) + " РІвЂ вЂ™ " + value)));
        feedback(source, Component.translatable("gui.jemcompat.keys.confirm").withStyle(style -> style.withUnderlined(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/jemkeys apply"))));
        return changes.size();
    }
    private static int apply() {
        if (Files.exists(marker())) return 0;
        var changes = changes();
        // Persist the one-time receipt first; a failed write must not mutate personal mappings.
        try {
            Files.createDirectories(marker().getParent());
            Files.writeString(marker(), REVISION + "\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (java.io.IOException failure) { throw new IllegalStateException("Cannot record JEM key preset choice", failure); }
        changes.forEach((mapping, value) -> {
            String[] parts = value.split(":", 2);
            mapping.setKeyModifierAndCode(parts.length == 1 ? KeyModifier.NONE : KeyModifier.valueOf(parts[1]), InputConstants.getKey(parts[0]));
        });
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        return changes.size();
    }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jemkeys")
                .executes(context -> preview(context.getSource()))
                .then(Commands.literal("preview").executes(context -> preview(context.getSource())))
                .then(Commands.literal("apply").executes(context -> {
                    int changed = apply();
                    feedback(context.getSource(), Component.translatable("gui.jemcompat.keys.result", changed));
                    return changed;
                })));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || initialized || Minecraft.getInstance().options == null) return;
        initialized = true;
        if (cleanOptions) apply();
    }
}
