package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatDeferredProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = ForgeHooks.class, remap = false)
public abstract class CombatForgeDispatchQuestMixin {
    @Inject(method = "onShieldBlock", at = @At("RETURN"))
    private static void jem$shield(net.minecraft.world.entity.LivingEntity actor, net.minecraft.world.damagesource.DamageSource source, float amount,
                                  CallbackInfoReturnable<net.minecraftforge.event.entity.living.ShieldBlockEvent> callback) {
        CombatDeferredProof.finish(callback.getReturnValue());
    }
    @Inject(method = "onRightClickBlock", at = @At("RETURN"))
    private static void jem$block(Player actor, InteractionHand hand, BlockPos pos, BlockHitResult hit, CallbackInfoReturnable<PlayerInteractEvent.RightClickBlock> callback) {
        CombatDeferredProof.finish(callback.getReturnValue());
    }
    @Inject(method = "onInteractEntity", at = @At("RETURN"))
    private static void jem$entity(Player actor, Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        CombatDeferredProof.finishEntity(actor, target, hand);
    }
}
