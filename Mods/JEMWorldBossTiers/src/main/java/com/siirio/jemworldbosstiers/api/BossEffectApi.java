package com.siirio.jemworldbosstiers.api;

import com.siirio.jemworldbosstiers.encounter.BossPeriodicEffects;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class BossEffectApi {
    public static void begin(LivingEntity boss) {
        BossPeriodicEffects.begin(boss);
    }

    public static void tick(LivingEntity boss,List<ServerPlayer> participants) {
        BossPeriodicEffects.tick(boss,participants);
    }

    public static void clear(LivingEntity boss) {
        BossPeriodicEffects.clear(boss);
    }

    private BossEffectApi() {}
}
