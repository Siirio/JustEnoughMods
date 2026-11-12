package dev.taste.engine.client;

import dev.taste.engine.TasteMod;
import dev.taste.engine.render.HandRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TasteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HandMaterialEvents {
    private HandMaterialEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void hand(RenderHandEvent event) {
        HandRenderer.observeNativeHand(event.getHand(), event.getItemStack(), event.isCanceled());
    }
}
