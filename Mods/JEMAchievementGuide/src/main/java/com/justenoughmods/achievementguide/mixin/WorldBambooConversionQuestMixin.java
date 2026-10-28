package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.worldmisc.WorldBambooProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.chybx.spideroverhaul.util.BambooGrowthHandler$ConversionTask", remap = false)
public abstract class WorldBambooConversionQuestMixin {
    @Unique private WorldBambooProof.Use jem$pollenUse;
    @Unique private BlockState jem$beforeConversion;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void jem$realNewTask(BlockPos root, CallbackInfo ci) { jem$pollenUse = WorldBambooProof.constructed(root); }
    @Inject(method = "replaceAt", at = @At("HEAD"))
    private void jem$beforeNativeReplace(ServerLevel level, BlockPos pos, CallbackInfo ci) { jem$beforeConversion = level.getBlockState(pos); }
    @Inject(method = "replaceAt", at = @At("RETURN"))
    private void jem$committedNativeReplace(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (jem$beforeConversion != null) WorldBambooProof.converted(jem$pollenUse, level, pos, jem$beforeConversion);
        jem$beforeConversion = null;
    }
}
