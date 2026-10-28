package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatEffectProof;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ThrowablePotionItem.class)
public abstract class CombatPotionThrowQuestMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void jem$begin(Level level, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) { CombatEffectProof.throwBegin(actor, actor.getItemInHand(hand)); }
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jem$spawn(Level level, Entity projectile) { boolean accepted = level.addFreshEntity(projectile); CombatEffectProof.thrown(projectile, accepted); return accepted; }
    @Inject(method = "use", at = @At("RETURN"))
    private void jem$end(Level level, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) { CombatEffectProof.throwEnd(callback.getReturnValue().getResult().consumesAction()); }
}
