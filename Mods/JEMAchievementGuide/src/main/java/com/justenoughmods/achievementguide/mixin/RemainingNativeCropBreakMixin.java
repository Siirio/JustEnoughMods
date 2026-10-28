package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeHarvestProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class RemainingNativeCropBreakMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private NativeHarvestProof.Run jem$harvest;
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void jem$begin(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        jem$harvest = null;
        if (RemainingFoodActors.real(player) != null) {
            var state = player.serverLevel().getBlockState(pos);
            jem$harvest = NativeHarvestProof.begin(player, pos, state, NativeHarvestProof.plantedKind(player, pos, state));
        }
    }
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void jem$end(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        var run = jem$harvest; jem$harvest = null;
        NativeHarvestProof.commit(run, callback.getReturnValue());
    }
}
