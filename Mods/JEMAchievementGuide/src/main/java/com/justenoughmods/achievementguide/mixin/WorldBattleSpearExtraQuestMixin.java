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
@Mixin(targets = "com.github.L_Ender.cataclysm.items.Ancient_Spear", remap = false)
public abstract class WorldBattleSpearExtraQuestMixin {
    @Inject(method = "launchTornado", at = @At("HEAD"))
    private void jem$begin0(ItemStack stack, LivingEntity actor, CallbackInfoReturnable<Boolean> ci) { WorldBattleExtraProof.begin(actor, stack, (Item)(Object)this, "spear"); }
    @Inject(method = "launchTornado", at = @At("RETURN"))
    private void jem$end0(ItemStack stack, LivingEntity actor, CallbackInfoReturnable<Boolean> ci) { WorldBattleExtraProof.end(); }
}
