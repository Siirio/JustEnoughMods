package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeCopperProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class HomeCopperHandQuestMixin {
    @Inject(method = "setItemSlot", at = @At("HEAD"))
    private void jem$assigned(EquipmentSlot slot, ItemStack stack, CallbackInfo callback) { HomeCopperProof.assigned((Entity)(Object)this, slot); }
}
