package com.justenoughmods.achievementguide.integration.treechop;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import ht.treechop.api.ChopEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TreeChopIntegration {
    private static final ResourceLocation TREE_FELLED = new ResourceLocation("jem_guide", "qol/8");
    private static final ResourceLocation MUSHROOM_FELLED = new ResourceLocation("jem_guide", "qol/8_3");

    private TreeChopIntegration() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(TreeChopIntegration.class);
    }

    @SubscribeEvent
    public static void onTreeFelled(ChopEvent.FinishChopEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        TreeChopAwards awards = TreeChopAwards.forFinishedChop(
                event.getFelled(),
                event.getChoppedBlockState().is(Blocks.MUSHROOM_STEM)
        );
        if (awards.isEmpty()) {
            return;
        }
        JemCriteria.fire(player, TREE_FELLED);
        if (awards == TreeChopAwards.TREE_AND_MUSHROOM) {
            JemCriteria.fire(player, MUSHROOM_FELLED);
        }
    }
}
