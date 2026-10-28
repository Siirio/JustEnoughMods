package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.nature.NatureDropProof;
import com.justenoughmods.achievementguide.integration.nature.NatureHarvestProof;
import com.justenoughmods.achievementguide.integration.nature.NatureFallingProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class NatureHarvestQuestMixin implements com.justenoughmods.achievementguide.integration.NativeHarvestWitness.Nature {
    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;
    @Unique private NatureHarvestProof.Harvest jem$natureHarvest;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void jem$beforeNatureHarvest(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        jem$natureHarvest = NatureHarvestProof.before(player, pos);
    }

    @Override public void jem$natureNativeHarvest(Block block, Level world, Player actor, BlockPos pos, BlockState state,
                                      BlockEntity blockEntity, ItemStack tool, Runnable original) {
        var harvest = jem$natureHarvest;
        if (harvest == null || actor != harvest.actor() || world != harvest.level() || !pos.equals(harvest.pos())
                || !state.equals(harvest.state())) {
            original.run();
        } else NatureDropProof.nativeCall(harvest.drop(), () -> {
            original.run();
            return null;
        });
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void jem$afterNatureHarvest(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        var harvest = jem$natureHarvest;
        jem$natureHarvest = null;
        NatureHarvestProof.after(harvest, callback.getReturnValue());
    }

    @Inject(method = "removeBlock", at = @At("RETURN"), remap = false)
    private void jem$releasedAnvil(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) NatureFallingProof.released(player, pos);
    }
}
