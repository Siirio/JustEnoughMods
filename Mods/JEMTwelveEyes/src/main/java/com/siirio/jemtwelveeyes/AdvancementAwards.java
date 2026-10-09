package com.siirio.jemtwelveeyes;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementAwards {
    private AdvancementAwards() {
    }

    public static void award(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement != null) {
            advancement.getCriteria().keySet().forEach(criterion -> player.getAdvancements().award(advancement, criterion));
        }
    }

    public static boolean completed(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
