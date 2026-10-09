package com.siirio.jemworldbosstiers.progression;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class TierAdvancements {
    private static final String CRITERION = "reached";

    private TierAdvancements() {
    }

    public static void awardCurrent(ServerPlayer player, int tier) {
        award(player, ResourceLocation.fromNamespaceAndPath(JemWorldBossTiers.MOD_ID, "world_tier/" + tier));
    }

    private static void award(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, CRITERION);
        }
    }
}
