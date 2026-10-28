package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldMaelstromProof;
import com.justenoughmods.achievementguide.integration.worldmisc.WorldNativeQuestProof;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.obscuria.aquamirae.common.item.NiveisTearItem", remap = false)
public abstract class WorldNiveisDrinkQuestMixin {
    @Inject(method = "m_5922_", at = @At("HEAD"))
    private void jem$drinkBegin(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        WorldMaelstromProof.beginDrink(stack, entity);
    }
    @Inject(method = "m_5922_", at = @At("RETURN"))
    private void jem$paidDrink(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        WorldMaelstromProof.endDrink(stack, entity);
    }
}
