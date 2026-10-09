package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.mixin.ExperienceOrbInvoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;

public final class ExperienceRewardApi {
    public static void give(ServerPlayer player, int experience) {
        if (experience <= 0) return;
        var orb = new ExperienceOrb(player.level(), player.getX(), player.getY(), player.getZ(), experience);
        int remaining = ((ExperienceOrbInvoker) orb).jem$repairPlayerItems(player, experience);
        if (remaining > 0) player.giveExperiencePoints(remaining);
    }

    private ExperienceRewardApi() {}
}
