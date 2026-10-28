package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeDollProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class HomeDollOpenQuestMixin {
    @Shadow @Final protected ServerPlayer player;
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void jem$before(BlockPos pos, CallbackInfoReturnable<Boolean> callback) { HomeDollProof.begin(player, pos); }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void jem$after(BlockPos pos, CallbackInfoReturnable<Boolean> callback) { HomeDollProof.finish(player, pos, callback.getReturnValue()); }
}
