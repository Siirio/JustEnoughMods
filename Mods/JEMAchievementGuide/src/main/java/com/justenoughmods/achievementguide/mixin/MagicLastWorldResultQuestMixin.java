package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastWorldProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class MagicLastWorldResultQuestMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void jem$begin(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> callback) { MagicLastWorldProof.blockBegin((Level)(Object)this, pos); }
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void jem$end(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> callback) { MagicLastWorldProof.blockEnd((Level)(Object)this, pos, callback.getReturnValue()); }
}
