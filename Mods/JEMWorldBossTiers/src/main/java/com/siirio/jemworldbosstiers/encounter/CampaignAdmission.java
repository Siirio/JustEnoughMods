package com.siirio.jemworldbosstiers.encounter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

/** Optional campaign owns its own prerequisite and missing-content decisions. */
public final class CampaignAdmission {
    private CampaignAdmission() { }

    public static Component blocked(ServerPlayer player, LivingEntity boss) {
        if (!ModList.get().isLoaded("jem_twelve_eyes")) return null;
        try {
            var api = Class.forName("com.siirio.jemtwelveeyes.api.CampaignApi");
            return (Component) api.getMethod("startBlockReason", net.minecraft.server.MinecraftServer.class,
                    net.minecraft.resources.ResourceLocation.class).invoke(null, player.server,
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Installed Twelve Eyes does not expose its campaign admission contract", exception);
        }
    }
}
