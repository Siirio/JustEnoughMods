package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.CoopFoodProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.farm_and_charm.core.block.ChickenCoopBlock;
import net.satisfy.farm_and_charm.core.block.entity.ChickenCoopBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ChickenCoopBlock.class, remap = false)
public abstract class ChickenCoopCollectQuestMixin {
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_36356_(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$nativeReceipt(Player actor, ItemStack eggs, BlockState state, Level level, BlockPos pos,
                                      Player nativeActor, InteractionHand hand, BlockHitResult hit) {
        if (actor instanceof ServerPlayer player && actor == nativeActor
                && level.getBlockEntity(pos) instanceof ChickenCoopBlockEntity owner)
            return CoopFoodProof.deliver(owner, player, eggs);
        return actor.addItem(eggs);
    }
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/satisfy/farm_and_charm/core/block/entity/ChickenCoopBlockEntity;clearEggs()V"))
    private void jem$nativeDecrement(ChickenCoopBlockEntity owner, BlockState state, Level level, BlockPos pos,
                                     Player actor, InteractionHand hand, BlockHitResult hit) {
        boolean received = actor instanceof ServerPlayer player && CoopFoodProof.consumeDelivery(owner, player);
        owner.clearEggs();
        if (received && actor instanceof ServerPlayer player && owner.getEggCount() == 0)
            QuestActionTrigger.INSTANCE.emit(player, "v3_food_coop_eggs");
    }
}
