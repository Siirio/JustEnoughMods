package com.justenoughmods.achievementguide.mixin;

import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.capabilities.magic.SummonManager", remap = false)
public interface SummonExpiryQuestAccess {
    @Invoker(value = "getExpirationTick", remap = false) int jem$nativeExpirationTick(UUID summon);
}
