package com.siirio.jemserver.client.smp;

import com.siirio.jemserver.smp.SmpUpdate;
import com.siirio.jemserver.smp.SmpNetwork;

import net.minecraft.client.Minecraft;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = "jem_server",
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class SmpClient {
    public static void accept(SmpUpdate update) {
        var minecraft = Minecraft.getInstance();
        if (update.open()) {
            if (minecraft.screen instanceof SmpScreen screen) screen.accept(update);
            else minecraft.setScreen(new SmpScreen(update));
        } else if (minecraft.screen instanceof SmpScreen screen) screen.accept(update);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("xaerominimap"))
            TemporaryWaypoints.tick(event);
        var client = Minecraft.getInstance();
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END
                && client.level != null
                && client.screen == null
                && CombatHud.active()
                && client.options.keyUse.isDown()
                && client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                && hit.getEntity() instanceof net.minecraft.world.entity.player.Player target)
            SmpNetwork.revive(target.getUUID());
    }

    public static void navigation(com.siirio.jemserver.smp.NavigationPoints points) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("xaerominimap"))
            TemporaryWaypoints.accept(points);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void logout(
            net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("xaerominimap"))
            TemporaryWaypoints.logout(event);
    }

    private SmpClient() {}
}
