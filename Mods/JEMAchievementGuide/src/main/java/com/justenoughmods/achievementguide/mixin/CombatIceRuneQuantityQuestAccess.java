package com.justenoughmods.achievementguide.mixin;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
@Pseudo
@Mixin(targets = "net.sweenus.simplybows.item.unique.IceBowItem", remap = false)
public interface CombatIceRuneQuantityQuestAccess {
    @Invoker("getArrowQuantity") int jem$nativeQuantity(BowUpgradeData data);
}
