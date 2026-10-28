package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.TomsStockSignalProof;
import com.tom.storagemod.tile.LevelEmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tom.storagemod.tile.LevelEmitterBlockEntity", remap = false)
public abstract class TomsLevelEmitterQuestMixin {
    @Unique private long jem$nativeCount;
    @Inject(method = "updateServer", at = @At("HEAD"))
    private void jem$beginNativeSample(CallbackInfo callback) { jem$nativeCount = 0; }
    @Redirect(method = "updateServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_41613_()I"))
    private int jem$nativeMatchingCount(ItemStack stack) { int count = stack.getCount(); jem$nativeCount += count; return count; }
    @Redirect(method = "updateServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean jem$nativePowerCommit(Level level, BlockPos position, BlockState declared, int flags) {
        BlockState before = level.getBlockState(position);
        boolean accepted = level.setBlock(position, declared, flags);
        if (accepted && level.getBlockState(position).equals(declared)) TomsStockSignalProof.committed((LevelEmitterBlockEntity) (Object) this, before, declared, jem$nativeCount);
        return accepted;
    }
    @Inject(method = "updateServer", at = @At("RETURN"))
    private void jem$nativeSampled(CallbackInfo callback) { TomsStockSignalProof.sampled((LevelEmitterBlockEntity) (Object) this, jem$nativeCount); }
    @Inject(method = "m_183515_", at = @At("TAIL"))
    private void jem$saveSignalProof(CompoundTag tag, CallbackInfo callback) {
        tag.put(TomsStockSignalProof.KEY, ((LevelEmitterBlockEntity) (Object) this).getPersistentData().getCompound(TomsStockSignalProof.KEY).copy());
    }
    @Inject(method = "m_142466_", at = @At("TAIL"))
    private void jem$loadSignalProof(CompoundTag tag, CallbackInfo callback) {
        ((LevelEmitterBlockEntity) (Object) this).getPersistentData().put(TomsStockSignalProof.KEY, tag.getCompound(TomsStockSignalProof.KEY).copy());
    }
}
