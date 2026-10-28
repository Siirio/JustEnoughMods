package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.processing.burner.BlazeBurnerBlock", remap = false)
public abstract class CreateBurnerHeatQuestMixin {
    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/burner/BlazeBurnerBlock;tryInsert(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;ZZZ)Lnet/minecraft/world/InteractionResultHolder;"), remap = false)
    private InteractionResultHolder<ItemStack> jem$actualFuel(BlockState state, Level world, BlockPos pos, ItemStack fuel,
            boolean creative, boolean overflow, boolean simulate, BlockState originalState, Level originalWorld, BlockPos originalPos,
            Player actor, InteractionHand hand, BlockHitResult hit) {
        int before = fuel.getCount();
        var heat = BlazeBurnerBlock.getHeatLevelOf(state);
        var result = BlazeBurnerBlock.tryInsert(state, world, pos, fuel, creative, overflow, simulate);
        if (!creative && !simulate && actor instanceof ServerPlayer player && result.getResult() == InteractionResult.SUCCESS
                && fuel.getCount() == before - 1 && world.getBlockEntity(pos) instanceof BlazeBurnerBlockEntity burner && !burner.isCreative()
                && BlazeBurnerBlock.getHeatLevelOf(world.getBlockState(pos)) != heat
                && BlazeBurnerBlock.getHeatLevelOf(world.getBlockState(pos)).isAtLeast(BlazeBurnerBlock.HeatLevel.KINDLED))
            QuestActionTrigger.INSTANCE.emit(player, "create_burner_fueled_heat");
        return result;
    }
}
