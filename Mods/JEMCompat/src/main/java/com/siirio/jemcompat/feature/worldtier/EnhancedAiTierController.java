package com.siirio.jemcompat.feature.worldtier;

import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.event.WorldTierChangedEvent;
import insane96mcp.insanelib.base.Module;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

public final class EnhancedAiTierController {
    private static final String ENHANCED_AI = "enhancedai";

    @SubscribeEvent
    public void serverAboutToStart(ServerAboutToStartEvent event) {
        if (ModList.get().isLoaded(ENHANCED_AI)) {
            setFeatureTier(1);
        }
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        if (ModList.get().isLoaded(ENHANCED_AI)) {
            int tier = WorldTierApi.currentTier(event.getServer());
            setFeatureTier(tier);
            reconcileLoadedMobs(event.getServer());
        }
    }

    @SubscribeEvent
    public void worldTierChanged(WorldTierChangedEvent event) {
        if (ModList.get().isLoaded(ENHANCED_AI)) {
            setFeatureTier(event.currentTier());
            event.server().execute(() -> reconcileLoadedMobs(event.server()));
        }
    }

    private static void setFeatureTier(int tier) {
        Module.getAllLoadedFeatures().forEach((type, feature) -> {
            if (EnhancedAiTierPolicy.managesModule(feature.getModule().getId().getNamespace())) {
                feature.setEnabled(EnhancedAiTierPolicy.enabledAt(type.getSimpleName(), tier));
            }
        });
    }

    private static void reconcileLoadedMobs(net.minecraft.server.MinecraftServer server) {
        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob && ordinaryMob(mob)) {
                    MinecraftForge.EVENT_BUS.post(new EntityJoinLevelEvent(mob, level));
                }
            }
        }
    }

    private static boolean ordinaryMob(Mob mob) {
        return !EnhancedAiTierPolicy.excludedNamespace(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace());
    }
}
