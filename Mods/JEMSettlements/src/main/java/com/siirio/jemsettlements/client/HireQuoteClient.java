package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.JemSettlements;
import net.minecraftforge.fml.ModList;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Authenticated server projection; never computes or authorizes a hire. */
@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT)
public final class HireQuoteClient {
    public record Quote(int entityId, long cost, long wage) { }
    private static Quote quote;
    private HireQuoteClient() { }
    public static void accept(int entityId, long cost, long wage) {
        var player = Minecraft.getInstance().player;
        if (cost < 0 || wage < 0 || player == null || !ModList.get().isLoaded("recruits") || !NativeHire.matches(player, entityId)) return;
        quote = new Quote(entityId, cost, wage);
    }
    public static Quote quote(int entityId) { return quote != null && quote.entityId() == entityId ? quote : null; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { quote = null; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var player = Minecraft.getInstance().player;
        if (event.phase == TickEvent.Phase.END && (player == null || !ModList.get().isLoaded("recruits") || !NativeHire.isOpen(player))) quote = null;
    }
    private static final class NativeHire {
        static boolean matches(net.minecraft.world.entity.player.Player player, int id) { return player.containerMenu instanceof com.talhanation.recruits.inventory.RecruitHireMenu menu && menu.getRecruitEntity().getId() == id; }
        static boolean isOpen(net.minecraft.world.entity.player.Player player) { return player.containerMenu instanceof com.talhanation.recruits.inventory.RecruitHireMenu; }
    }
}
