package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.battle.BattleQuestArtifacts;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.items.Void_core", remap = false)
public abstract class WorldBattleVoidCoreQuestMixin {
    @Inject(method = "m_7203_", at = @At("HEAD"))
    private void jem$begin(Level level, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) { BattleQuestArtifacts.begin(actor, actor.getItemInHand(hand), "earth"); }
    @Inject(method = "m_7203_", at = @At("RETURN"))
    private void jem$end(Level level, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) { BattleQuestArtifacts.end(); }
}
