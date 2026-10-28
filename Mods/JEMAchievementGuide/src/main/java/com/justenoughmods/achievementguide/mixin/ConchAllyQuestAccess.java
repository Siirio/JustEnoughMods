package com.justenoughmods.achievementguide.mixin;

import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.entity.living.DeepOneBaseEntity", remap = false)
public interface ConchAllyQuestAccess {
    @Accessor("summonerUUID") UUID jem$nativeSummoner();
    @Invoker("isSummoned") boolean jem$nativeSummoned();
    @Invoker("getSummonTime") int jem$nativeSummonTime();
}
