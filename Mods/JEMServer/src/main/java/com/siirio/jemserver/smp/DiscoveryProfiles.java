package com.siirio.jemserver.smp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class DiscoveryProfiles {
    private static final String TAME_RECEIPT = "jem_profile_tame_recorded";

    @SubscribeEvent
    public static void explored(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player instanceof FakePlayer
                || !player.isAlive()) return;
        int interval = SmpConfig.DISCOVERY_INTERVAL.get();
        if (Math.floorMod(player.server.getTickCount(), interval)
                != Math.floorMod(player.getUUID().hashCode(), interval)) return;
        var biome = player.serverLevel().getBiome(player.blockPosition()).unwrapKey();
        if (biome.isEmpty()) return;
        var profile = Profiles.get(player.server, player.getUUID());
        var visited = profile.getCompound("discoveredBiomes");
        String id = biome.get().location().toString();
        if (visited.getBoolean(id)) return;
        visited.putBoolean(id, true);
        profile.put("discoveredBiomes", visited);
        profile.putLong("biomesExplored", visited.size());
        SmpData.get(player.server).changed(profile);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tamed(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer player) || player instanceof FakePlayer)
            return;
        var entity = event.getAnimal();
        var receipt = entity.getPersistentData();
        if (receipt.getBoolean(TAME_RECEIPT)) return;
        var type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (type == null) return;
        receipt.putBoolean(TAME_RECEIPT, true);
        var profile = Profiles.get(player.server, player.getUUID());
        CompoundTag species = profile.getCompound("tamedSpecies");
        species.putBoolean(type.toString(), true);
        profile.put("tamedSpecies", species);
        profile.putLong("tameSpecies", species.size());
        profile.putLong("animalsTamed", profile.getLong("animalsTamed") + 1);
        SmpData.get(player.server).changed(profile);
    }

    private DiscoveryProfiles() {}
}
