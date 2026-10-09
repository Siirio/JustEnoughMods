package com.siirio.jemserver.smp.fishing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class FishingProfiles {
    public record Profile(double quality, boolean nether, boolean end, boolean intrinsicLava,
                          boolean intrinsicVoid) {}
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final java.lang.reflect.Type TYPE = new TypeToken<LinkedHashMap<String, Profile>>() {}.getType();
    private static final Profile BASIC = new Profile(0, false, false, false, false);
    private static Map<String, Profile> profiles = new LinkedHashMap<>();
    private static boolean clientActive;

    private static Map<String, Profile> defaults() {
        try (var stream = FishingProfiles.class.getResourceAsStream("/data/jem_server/fishing/rods.json")) {
            if (stream == null) throw new IllegalStateException("Missing fishing profiles");
            return JSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), TYPE);
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }

    @SubscribeEvent
    public static void load(ServerStartedEvent event) {
        profiles = defaults();
        var path = FMLPaths.CONFIGDIR.get().resolve("jem/generated/fishing_rods.json");
        try {
            if (Files.exists(path)) {
                try (var reader = Files.newBufferedReader(path)) {
                    Map<String, Profile> configured = JSON.fromJson(reader, TYPE);
                    configured.forEach((id, profile) -> { validate(profile); profiles.put(id, profile); });
                }
            }
            ForgeRegistries.ITEMS.forEach(item -> {
                ItemStack stack = item.getDefaultInstance();
                if (item instanceof FishingRodItem || isRod(stack))
                    profiles.putIfAbsent(ForgeRegistries.ITEMS.getKey(item).toString(), BASIC);
            });
            Files.createDirectories(path.getParent());
            Files.writeString(path, JSON.toJson(profiles), StandardCharsets.UTF_8);
            var pools = new com.google.gson.JsonObject();
            var operations = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, event.getServer().registryAccess());
            com.li64.tide.data.TideData.FISH.get().stream().sequential().forEach(entry -> {
                var encoded = com.li64.tide.data.fishing.FishData.CODEC.encodeStart(operations, entry.getValue())
                        .getOrThrow(false, message -> { throw new IllegalStateException(message); });
                pools.add(entry.getKey().toString(), encoded);
            });
            Files.writeString(path.resolveSibling("fish_pools.json"), JSON.toJson(pools), StandardCharsets.UTF_8);
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot load fishing profiles", error); }
    }

    private static void validate(Profile profile) {
        if (profile == null || !Double.isFinite(profile.quality()) || profile.quality() < 0 || profile.quality() > 1)
            throw new IllegalArgumentException("Invalid fishing profile");
    }

    public static boolean isRod(ItemStack stack) {
        return stack.getItem() instanceof FishingRodItem || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("tide", "fishing_rods")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/fishing_rods")))
                || stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("c", "tools/fishing_rod")));
    }
    public static Profile get(ItemStack rod) {
        if (rod == null || rod.isEmpty()) return BASIC;
        return profiles.getOrDefault(ForgeRegistries.ITEMS.getKey(rod.getItem()).toString(), BASIC);
    }
    public static double[] chances(ItemStack rod) {
        double quality = get(rod).quality();
        return new double[]{60 - 20 * quality, 25 - 5 * quality, 10 + 8 * quality, 4 + 10 * quality, 1 + 7 * quality};
    }
    public static String encode() { return JSON.toJson(profiles); }
    public static void accept(String json) {
        Map<String, Profile> incoming = JSON.fromJson(json, TYPE);
        if (incoming == null || incoming.size() > 1024) throw new IllegalArgumentException("Invalid fishing profile count");
        incoming.values().forEach(FishingProfiles::validate);
        profiles = incoming;
        clientActive = true;
    }
    public static boolean clientActive() { return clientActive; }
    public static void clearClient() { profiles = new LinkedHashMap<>(); clientActive = false; }
    private FishingProfiles() {}
}
