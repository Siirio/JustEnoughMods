package com.siirio.jemcompat.feature.spawner;

import com.google.gson.JsonParser;
import com.siirio.jemcompat.JEMCompat;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** One reviewed policy shared by recipes, player spawner programming and spawner ticking. */
public final class SpawnEggPolicy {
    public enum SpawnerRule { YES, NO, SLOW_24000_TICKS }

    private record Entry(ResourceLocation entity, ResourceLocation egg, SpawnerRule spawner) { }
    private static final Map<ResourceLocation, Entry> BY_EGG = new HashMap<>();
    private static final Map<ResourceLocation, Entry> BY_ENTITY = new HashMap<>();

    static {
        try (var stream = SpawnEggPolicy.class.getResourceAsStream("/data/jemcompat/spawn_egg_policy.json")) {
            if (stream == null) throw new IllegalStateException("Missing spawn egg policy");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : root.getAsJsonArray("entries")) {
                var row = element.getAsJsonObject();
                var entry = new Entry(new ResourceLocation(row.get("entity").getAsString()),
                        new ResourceLocation(row.get("egg").getAsString()),
                        SpawnerRule.valueOf(row.get("spawner").getAsString()));
                if (BY_EGG.put(entry.egg(), entry) != null || BY_ENTITY.put(entry.entity(), entry) != null)
                    throw new IllegalStateException("Duplicate spawn egg policy: " + row);
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot load spawn egg policy", failure);
        }
    }

    private SpawnEggPolicy() { }

    public static SpawnerRule forEntity(ResourceLocation entity) {
        Entry entry = BY_ENTITY.get(entity);
        return entry == null ? SpawnerRule.YES : entry.spawner();
    }

    @SubscribeEvent
    public static void useEggOnSpawner(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().getBlockState(event.getPos()).getBlock() != Blocks.SPAWNER) return;
        ResourceLocation item = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        Entry entry = item == null ? null : BY_EGG.get(item);
        if (entry == null || entry.spawner() != SpawnerRule.NO) return;
        if (!event.getLevel().isClientSide())
            event.getEntity().sendSystemMessage(Component.translatable("message." + JEMCompat.MOD_ID + ".spawner_denied"));
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }
}
