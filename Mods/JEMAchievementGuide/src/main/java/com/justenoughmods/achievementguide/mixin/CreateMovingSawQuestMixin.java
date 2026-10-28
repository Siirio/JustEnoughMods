package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateMovingHarvest;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.kinetics.saw.SawMovementBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour", remap = false)
public abstract class CreateMovingSawQuestMixin {
    @Redirect(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/BlockHelper;destroyBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;FLjava/util/function/Consumer;)V"), remap = false)
    private void jem$nativeSawLog(Level world, BlockPos pos, float chance, Consumer<ItemStack> output,
                                  MovementContext context, BlockPos target) {
        if ((Object)this instanceof SawMovementBehaviour) {
            CreateMovingHarvest.harvested(context, pos, false, () -> BlockHelper.destroyBlock(world, pos, chance, output));
        } else BlockHelper.destroyBlock(world, pos, chance, output);
    }
    @Redirect(method = "lambda$destroyBlock$0", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/base/BlockBreakingMovementBehaviour;dropItem(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/world/item/ItemStack;)V"), remap = false)
    private void jem$actualSawOutput(BlockBreakingMovementBehaviour behaviour, MovementContext context, ItemStack item) {
        CreateMovingHarvest.dropped((MovementBehaviour)behaviour, context, item);
    }
}
