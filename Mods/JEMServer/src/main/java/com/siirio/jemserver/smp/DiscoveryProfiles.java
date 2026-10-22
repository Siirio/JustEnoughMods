package com.siirio.jemserver.smp;

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
        if (!(event.player instanceof ServerPlayer)) return;
        var data = SmpData.get(((ServerPlayer) event.player).server);
        data.edit(() -> exploredOwned(event));
    }

    private static void exploredOwned(TickEvent.PlayerTickEvent event) {
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
        var visited = profile.discoveredBiomes();
        String id = biome.get().location().toString();
        if (!profile.discoverBiome(id)) return;
        profile.biomesExplored(profile.discoveredBiomes().size());
        
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tamed(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer)) return;
        var data = SmpData.get(((ServerPlayer) event.getTamer()).server);
        data.edit(() -> tamedOwned(event));
    }

    private static void tamedOwned(AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer player) || player instanceof FakePlayer)
            return;
        var entity = event.getAnimal();
        var receipt = entity.getPersistentData();
        if (receipt.getBoolean(TAME_RECEIPT)) return;
        var type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (type == null) return;
        receipt.putBoolean(TAME_RECEIPT, true);
        var profile = Profiles.get(player.server, player.getUUID());
        profile.tameSpecies(type.toString());
        profile.tameSpecies(profile.tamedSpecies().size());
        profile.animalsTamed(profile.animalsTamed() + 1);
        
    }

    private DiscoveryProfiles() {}
}
