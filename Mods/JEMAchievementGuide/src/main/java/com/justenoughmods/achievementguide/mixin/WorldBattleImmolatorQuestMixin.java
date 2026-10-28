package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.BattleQuestArtifacts;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.items.The_Immolator", remap = false)
public abstract class WorldBattleImmolatorQuestMixin {
    @Inject(method = "m_5551_", at = @At("HEAD"))
    private void jem$begin(ItemStack stack, Level level, LivingEntity actor, int remaining, CallbackInfo ci) { BattleQuestArtifacts.begin(actor, stack, "charge"); }
    @Inject(method = "m_5551_", at = @At("RETURN"))
    private void jem$end(ItemStack stack, Level level, LivingEntity actor, int remaining, CallbackInfo ci) { BattleQuestArtifacts.end(); }
}
