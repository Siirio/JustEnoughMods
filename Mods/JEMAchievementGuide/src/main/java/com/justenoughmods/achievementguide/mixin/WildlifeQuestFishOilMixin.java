package com.justenoughmods.achievementguide.mixin;

import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.wildlife.*;
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
@Mixin(targets = "com.github.alexthe666.alexsmobs.item.ItemFishOil", remap = false)
public abstract class WildlifeQuestFishOilMixin {
    @Unique private final ThreadLocal<ItemStack> jem$oil = new ThreadLocal<>();
    @Inject(method = "m_5922_", at = @At("HEAD"))
    private void jem$before(ItemStack stack, Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> callback) { jem$oil.set(stack.copy()); }
    @Inject(method = "m_5922_", at = @At("RETURN"))
    private void jem$after(ItemStack stack, Level world, LivingEntity user, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack before = jem$oil.get(); jem$oil.remove();
        if (before == null || !(user instanceof net.minecraft.world.entity.player.Player player)) return; var actor = WildlifeQuestProof.real(player);
        if (actor != null && actor.hasEffect(AMEffectRegistry.OILED.get()) && actor.getUUID().equals(NativeFoodSources.get(actor.serverLevel()).owner(before, "wildlife_oil"))) {
            var origin = WildlifeQuestTools.get(actor.serverLevel()).origin(before);
            if (origin != null) { WildlifeQuestProof.data(actor).putUUID("oiled_craft", origin); WildlifeQuestProof.data(actor).putLong("oiled_until", world.getGameTime() + actor.getEffect(AMEffectRegistry.OILED.get()).getDuration()); }
        }
    }
}
