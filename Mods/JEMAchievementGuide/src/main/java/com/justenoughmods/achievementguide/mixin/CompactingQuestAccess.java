package com.justenoughmods.achievementguide.mixin;

import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeWrapper", remap = false)
public interface CompactingQuestAccess {
    @Invoker(value = "compactSlotAndQueued", remap = false)
    void jem$nativeCompactSlotAndQueued(IItemHandlerSimpleInserter inventory, int slot);
}
