package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.food.NativePlantProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.primal.block.FruitSapling", remap = false)
public abstract class RemainingFruitSaplingGrowthMixin {
    @Shadow @Final private RegistryObject<Block> fruitTree;
    @Unique private final ThreadLocal<NativeFoodSources.Plant> jem$planted = new ThreadLocal<>();
    @Inject(method = "growFruitTree", at = @At("HEAD"))
    private void jem$begin(Level level, BlockPos pos, CallbackInfo callback) {
        jem$planted.remove();
        if (level instanceof ServerLevel server) {
            var planted = NativePlantProof.original(server, pos, server.getBlockState(pos));
            if (planted != null) jem$planted.set(planted);
        }
    }
    @Inject(method = "growFruitTree", at = @At("RETURN"))
    private void jem$grown(Level level, BlockPos pos, CallbackInfo callback) {
        var planted = jem$planted.get(); jem$planted.remove();
        if (planted != null && level instanceof ServerLevel server && server.getBlockState(pos).is(fruitTree.get())) {
            NativePlantProof.evolved(server, pos, planted, server.getBlockState(pos));
        }
    }
}
