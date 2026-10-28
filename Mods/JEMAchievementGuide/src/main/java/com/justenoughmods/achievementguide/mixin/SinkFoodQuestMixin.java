package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.FoodOutputProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.farm_and_charm.core.block.SinkBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SinkBlock.class, remap = false)
public abstract class SinkFoodQuestMixin {
    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_36356_(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean jem$nativeWater(Player actor, ItemStack output, BlockState state, Level level, BlockPos pos,
                                     Player nativeActor, InteractionHand hand, BlockHitResult hit) {
        ItemStack expected = output.copy();
        boolean water = actor instanceof ServerPlayer && actor == nativeActor
                && state.getValue(SinkBlock.HALF) == DoubleBlockHalf.LOWER && state.getValue(SinkBlock.FILLED)
                && (expected.is(Items.WATER_BUCKET) || expected.is(Items.POTION) && PotionUtils.getPotion(expected) == Potions.WATER)
                && level.getBlockState(pos).equals(state.setValue(SinkBlock.FILLED, false));
        int held = water ? FoodOutputProof.held((ServerPlayer)actor, ((ServerPlayer)actor).containerMenu, expected) : 0;
        boolean result = actor.addItem(output);
        if (water && FoodOutputProof.held((ServerPlayer)actor, ((ServerPlayer)actor).containerMenu, expected) > held)
            QuestActionTrigger.INSTANCE.emit((ServerPlayer)actor, "food_sink_water");
        return result;
    }
}
