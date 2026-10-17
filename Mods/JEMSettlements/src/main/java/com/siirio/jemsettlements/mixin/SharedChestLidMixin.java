package com.siirio.jemsettlements.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChestBlockEntity.class)
public abstract class SharedChestLidMixin {
    // Exact native signalOpenCount newCount argument, preserving the real player counter.
    @ModifyVariable(method="signalOpenCount",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private int jem$keepResidentLidOpen(int count,Level level,BlockPos pos,BlockState state,int oldCount,int newCount) {
        return com.siirio.jemsettlements.runtime.SharedStorageService.isOpen(level,pos)?Math.max(1,count):count;
    }
}
