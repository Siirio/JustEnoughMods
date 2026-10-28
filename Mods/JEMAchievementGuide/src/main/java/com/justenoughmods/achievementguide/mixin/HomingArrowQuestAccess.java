package com.justenoughmods.achievementguide.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = {"net.sweenus.simplybows.entity.HomingArrowEntity",
        "net.sweenus.simplybows.entity.HomingSpectralArrowEntity"}, remap = false)
public interface HomingArrowQuestAccess {
    @Accessor(value = "homingEnabled", remap = false)
    boolean jem$nativeHomingEnabled();
}
