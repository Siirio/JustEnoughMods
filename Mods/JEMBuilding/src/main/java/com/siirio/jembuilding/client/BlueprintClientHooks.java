package com.siirio.jembuilding.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;

public final class BlueprintClientHooks {
    public static void openCapture(BlockPos second, InteractionHand hand) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || (com.siirio.jembuilding.item.BlueprintRollItem.first(client.player.getItemInHand(hand)).isEmpty()
                && BlueprintInput.pendingFirst() == null)) return;
        BlueprintClientAnimation.beginSketch(second, hand);
    }
    private BlueprintClientHooks() { }
}
