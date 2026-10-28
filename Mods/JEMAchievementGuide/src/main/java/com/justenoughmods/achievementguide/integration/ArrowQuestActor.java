package com.justenoughmods.achievementguide.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;

/** Same native projectile accepted by the actual player's bow or crossbow release. */
public final class ArrowQuestActor {
    private ArrowQuestActor() {}
    public static ServerPlayer get(Projectile arrow) {
        var proof = arrow.getPersistentData();
        if (!(arrow.getOwner() instanceof ServerPlayer actor)) return null;
        boolean bow = proof.hasUUID("JemQuestNativeBowActorV4")
                && actor.getUUID().equals(proof.getUUID("JemQuestNativeBowActorV4"));
        boolean crossbow = proof.hasUUID("JemQuestNativeCrossbowActorV4")
                && actor.getUUID().equals(proof.getUUID("JemQuestNativeCrossbowActorV4"));
        return bow || crossbow ? actor : null;
    }
}
