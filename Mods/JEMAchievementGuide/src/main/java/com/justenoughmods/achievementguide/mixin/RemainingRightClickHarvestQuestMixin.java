package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeHarvestProof;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
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
@Mixin(targets = "io.github.jamalam360.rightclickharvest.RightClickHarvest", remap = false)
public abstract class RemainingRightClickHarvestQuestMixin {
    @Unique private static final ThreadLocal<NativeHarvestProof.Run> jem$harvest = new ThreadLocal<>();
    @Inject(method = "completeHarvest", at = @At("HEAD"))
    private static void jem$begin(Level level, BlockState state, BlockPos pos, Player player, InteractionHand hand, ItemStack tool,
                                  boolean hoe, boolean replant, Runnable action, CallbackInfoReturnable<InteractionResult> callback) {
        jem$harvest.remove();
        var actor = RemainingFoodActors.real(player);
        if (actor != null && replant && NativeHarvestProof.mature(state)) jem$harvest.set(NativeHarvestProof.begin(actor, pos, state, "harvest_again"));
    }
    @Inject(method = "completeHarvest", at = @At("RETURN"))
    private static void jem$end(Level level, BlockState state, BlockPos pos, Player player, InteractionHand hand, ItemStack tool,
                                boolean hoe, boolean replant, Runnable action, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$harvest.get(); jem$harvest.remove();
        NativeHarvestProof.commit(run, callback.getReturnValue() == InteractionResult.SUCCESS);
    }
}
