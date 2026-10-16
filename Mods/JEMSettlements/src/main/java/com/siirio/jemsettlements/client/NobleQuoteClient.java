package com.siirio.jemsettlements.client;

import com.siirio.jemsettlements.JemSettlements;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, value = Dist.CLIENT)
public final class NobleQuoteClient {
    public record Quote(long cost, long wage) { }
    private static int nobleId = -1;
    private static Quote hoveredPrice;
    private static long spendable, receivedAt;
    private static Map<ResourceLocation, Quote> quotes = Map.of();
    private NobleQuoteClient() { }
    public interface ScreenAccess { int jem$nobleId(); }
    public static void accept(CompoundTag view) {
        var level = Minecraft.getInstance().level;
        if (level == null || view.getLong("Spendable") < 0) return;
        Map<ResourceLocation, Quote> next = new HashMap<>();
        for (Tag value : view.getList("Quotes", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value; var id = ResourceLocation.tryParse(row.getString("Id"));
            if (id != null && row.getLong("Cost") > 0 && row.getLong("Wage") >= 0) next.put(id, new Quote(row.getLong("Cost"), row.getLong("Wage")));
        }
        nobleId = view.getInt("NobleId"); spendable = view.getLong("Spendable"); quotes = Map.copyOf(next); receivedAt = level.getGameTime();
    }
    public static Quote quote(int entityId, ResourceLocation type) { return entityId == nobleId ? quotes.get(type) : null; }
    public static Quote current(ResourceLocation type) {
        return Minecraft.getInstance().screen instanceof ScreenAccess screen ? quote(screen.jem$nobleId(), type) : null;
    }
    public static void hoverPrice(Quote quote) { hoveredPrice = quote; }
    public static Quote takePriceHover() { var result = hoveredPrice; hoveredPrice = null; return result; }
    public static boolean affordable(Quote quote) { return quote != null && spendable >= quote.cost(); }
    private static void clear() { nobleId = -1; quotes = Map.of(); hoveredPrice = null; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || (!(mc.screen instanceof ScreenAccess) && mc.level.getGameTime() - receivedAt > 40)) clear();
    }
}
