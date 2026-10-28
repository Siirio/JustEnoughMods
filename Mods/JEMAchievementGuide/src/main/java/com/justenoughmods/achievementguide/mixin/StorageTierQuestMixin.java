package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.StorageTierQuestProof;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem", remap = false)
public abstract class StorageTierQuestMixin {
    @Unique private StorageTierQuestProof.Before jem$tierBefore;
    @Inject(method = "tryUpgradeStorage(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"))
    private void jem$beforeTier(ItemStack payment, Level level, BlockPos position, BlockState state, Player actor, CallbackInfoReturnable<InteractionResult> callback) {
        jem$tierBefore = StorageTierQuestProof.before(payment, level, position, state, actor);
    }
    @Inject(method = "tryUpgradeStorage(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;", at = @At("RETURN"))
    private void jem$afterTier(ItemStack payment, Level level, BlockPos position, BlockState state, Player actor, CallbackInfoReturnable<InteractionResult> callback) {
        var before = jem$tierBefore;
        jem$tierBefore = null;
        StorageTierQuestProof.after(payment, level, actor, callback.getReturnValue(), before);
    }
}
