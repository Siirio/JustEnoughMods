package com.justenoughmods.achievementguide.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.CookingLogic", remap = false)
public interface CookingQuestAccess {
    @Accessor(value = "upgrade", remap = false) ItemStack jem$questCookingUpgrade();
    @Invoker(value = "save", remap = false) void jem$saveQuestCookingUpgrade();
}
