package com.siirio.jembuilding.client;

import com.siirio.jembuilding.JEMBuilding;
import com.siirio.jembuilding.item.BlueprintRollItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMBuilding.MOD_ID, value = Dist.CLIENT)
public final class BlueprintClientAnimation {
    private static final int SKETCH_TICKS = 25;
    private static PendingCapture pendingCapture;
    private static int sketchTicks;

    static void playOpen(InteractionHand hand) {
        PunchyBlueprintBridge.play(hand, "jem_blueprint_open_right");
    }

    static void beginSketch(BlockPos second, InteractionHand hand) {
        if (pendingCapture != null) return;
        if (!PunchyBlueprintBridge.play(hand, "jem_blueprint_sketch_right")) {
            Minecraft.getInstance().setScreen(new BlueprintCaptureScreen(second, hand));
            return;
        }
        pendingCapture = new PendingCapture(second.immutable(), hand);
        sketchTicks = SKETCH_TICKS;
    }

    public static boolean shouldRenderOpen(ItemStack stack) {
        return sketchTicks > 0 || BlueprintRollItem.first(stack).isPresent() || BlueprintInput.pendingFirst() != null;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingCapture == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !(client.player.getItemInHand(pendingCapture.hand()).getItem() instanceof BlueprintRollItem)) {
            clear();
            return;
        }
        if (--sketchTicks > 0) return;
        PendingCapture capture = pendingCapture;
        clear();
        client.setScreen(new BlueprintCaptureScreen(capture.second(), capture.hand()));
    }

    private static void clear() {
        pendingCapture = null;
        sketchTicks = 0;
    }

    private record PendingCapture(BlockPos second, InteractionHand hand) { }

    private BlueprintClientAnimation() { }
}
