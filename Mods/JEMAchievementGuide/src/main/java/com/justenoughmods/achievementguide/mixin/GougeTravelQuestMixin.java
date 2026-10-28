package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.travel.GougeTravelProof;
import java.util.UUID;
import java.util.Set;
import net.gouge.GougePhysics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GougePhysics.class, remap = false)
public abstract class GougeTravelQuestMixin {
    @Shadow @Final private static Set<UUID> gougeNoGravity;
    @Inject(method = "applyImpactDamage(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;DF)Z", at = @At("RETURN"))
    private static void jem$nativeCatch(ItemStack stack, ServerLevel level, ServerPlayer actor, double speed, float hardness,
                                      CallbackInfoReturnable<Boolean> callback) {
        GougeTravelProof.impact(actor, callback.getReturnValue(), speed);
    }

    @Inject(method = "tick0(Lnet/minecraft/server/level/ServerPlayer;[I)Z", at = @At("RETURN"))
    private static void jem$nativeSupport(ServerPlayer actor, int[] nativeState, CallbackInfoReturnable<Boolean> callback) {
        GougeTravelProof.supported(actor, callback.getReturnValue(), GougePhysics.isActive(actor.getUUID())
                && gougeNoGravity.contains(actor.getUUID()));
    }

    @Inject(method = "wallKick(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/BlockHitResult;[I)Z", at = @At("RETURN"))
    private static void jem$nativeKick(ServerPlayer actor, ServerLevel level, BlockHitResult hit, int[] nativeState,
                                     CallbackInfoReturnable<Boolean> callback) {
        GougeTravelProof.kicked(actor, callback.getReturnValue());
    }

    @Inject(method = {"cleanup(Ljava/util/UUID;)V", "clearActive(Ljava/util/UUID;)V"}, at = @At("HEAD"))
    private static void jem$nativeCleanup(UUID actor, CallbackInfo callback) { GougeTravelProof.reset(actor); }

    @Inject(method = "stopSliding(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private static void jem$nativeStop(ServerPlayer actor, CallbackInfo callback) { GougeTravelProof.reset(actor.getUUID()); }
}
