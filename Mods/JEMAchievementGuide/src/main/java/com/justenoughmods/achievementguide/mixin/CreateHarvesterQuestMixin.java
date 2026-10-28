package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateMovingHarvest;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour", remap = false)
public abstract class CreateHarvesterQuestMixin {
    @Redirect(method = "visitNewPosition", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/BlockHelper;destroyBlockAs(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;FLjava/util/function/Consumer;)V"), remap = false)
    private void jem$nativeMatureHarvest(Level world, BlockPos pos, Player player, ItemStack tool, float chance,
                                         Consumer<ItemStack> output, MovementContext context, BlockPos target) {
        CreateMovingHarvest.harvested(context, pos, true, () -> BlockHelper.destroyBlockAs(world, pos, player, tool, chance, output));
    }
    @Redirect(method = "lambda$visitNewPosition$0", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/actors/harvester/HarvesterMovementBehaviour;dropItem(Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/world/item/ItemStack;)V"), remap = false)
    private void jem$actualHarvestOutput(com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour behaviour,
                                         MovementContext context, ItemStack item) {
        CreateMovingHarvest.dropped((MovementBehaviour)behaviour, context, item);
    }
}
