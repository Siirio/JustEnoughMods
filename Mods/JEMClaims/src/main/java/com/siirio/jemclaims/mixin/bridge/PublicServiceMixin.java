package com.siirio.jemclaims.mixin.bridge;

import com.siirio.jemclaims.FlanBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "io.github.flemmli97.flan.event.BlockInteractEvents", remap = false)
public abstract class PublicServiceMixin {
    @Inject(method = "useBlocks", at = @At("HEAD"), cancellable = true)
    private static void jemClaims$publicService(Player player, Level level, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (level instanceof ServerLevel serverLevel && !(player instanceof net.minecraftforge.common.util.FakePlayer)
                && FlanBridge.publicService(serverLevel, hit.getBlockPos())) callback.setReturnValue(InteractionResult.PASS);
    }
}
