package dev.taste.engine.client;

import dev.taste.engine.TasteMod;
import dev.taste.engine.render.TasteRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT)
public final class RenderDiagnosticsOverlay {
    private RenderDiagnosticsOverlay() { }

    @SubscribeEvent
    public static void postGui(RenderGuiEvent.Post event) {
        String label = TasteRenderer.instance().overlayText();
        if (label.isBlank()) return;
        event.getGuiGraphics().drawString(Minecraft.getInstance().font, label, 8, 8, 0xFFF1D8, true);
    }
}
