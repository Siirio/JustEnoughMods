package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.WorldBattleExtraProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.items.Infernal_forge", remap = false)
public abstract class WorldBattleInfernalExtraQuestMixin {
    @Inject(method = "m_6225_", at = @At("HEAD"))
    private void jem$begin0(UseOnContext context, CallbackInfoReturnable<InteractionResult> ci) { WorldBattleExtraProof.begin(context.getPlayer(), context.getItemInHand(), (Item)(Object)this, "infernal"); }
    @Inject(method = "m_6225_", at = @At("RETURN"))
    private void jem$end0(UseOnContext context, CallbackInfoReturnable<InteractionResult> ci) { WorldBattleExtraProof.end(); }
}
