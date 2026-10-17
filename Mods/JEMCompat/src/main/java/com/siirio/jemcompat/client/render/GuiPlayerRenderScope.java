package com.siirio.jemcompat.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Screen rendering starts after the world pass; frame-start reset also covers cancelled screens. */
@Mod.EventBusSubscriber(modid = "jemcompat", value = Dist.CLIENT)
public final class GuiPlayerRenderScope {
    private static boolean active;
    private GuiPlayerRenderScope() { }
    public static boolean active() { return active; }
    public static void reset() { active = false; }
    @SubscribeEvent public static void begin(ScreenEvent.Render.Pre event) { active = true; }
    @SubscribeEvent public static void end(ScreenEvent.Render.Post event) { reset(); }
}
