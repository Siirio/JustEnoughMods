package com.justenoughmods.achievementguide.mixin;

import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.CachedFailedInsertInventoryHandler", remap = false)
public interface CachedInventoryQuestAccess {
    @Accessor(value = "wrappedHandlerGetter", remap = false) Supplier<?> jem$nativeCachedInventory();
}
