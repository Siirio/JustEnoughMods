package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldNativeQuestProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.min01.oceanicrealms.entity.AbstractOceanicShark", remap = false)
public abstract class WorldSharkMealQuestMixin {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;m_146870_()V"))
    private void jem$nativeMouthConsumed(ItemEntity drop) { WorldNativeQuestProof.sharkConsumed((Entity)(Object)this, drop); }
}
