package com.justenoughmods.achievementguide.integration;

import com.ordana.immersive_weathering.util.WeatheringHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ModCompatibilityEvents {
    private ModCompatibilityEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAxeStrip(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.isCanceled() || event.getToolAction() != ToolActions.AXE_STRIP || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState original = event.getState();
        BlockState result = event.getFinalState();
        if (result == original) {
            result = original.getBlock().getToolModifiedState(original, event.getContext(), ToolActions.AXE_STRIP, false);
        }
        if (result == null || result.is(original.getBlock())) {
            return;
        }
        Item bark = WeatheringHelper.getBarkToStrip(original);
        Player player = event.getPlayer();
        if (bark == null || player != null && player.getAbilities().instabuild) {
            return;
        }
        BlockPos pos = event.getPos();
        Block.popResourceFromFace(level, pos, event.getContext().getClickedFace(), new ItemStack(bark));
    }
}
