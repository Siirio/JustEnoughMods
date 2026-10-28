package com.justenoughmods.achievementguide.mixin;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase", remap = false)
public interface UpgradeWrapperQuestAccess {
    @Accessor(value = "storageWrapper", remap = false)
    IStorageWrapper jem$questStorageWrapper();
}
