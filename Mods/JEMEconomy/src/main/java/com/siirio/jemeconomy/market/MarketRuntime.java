package com.siirio.jemeconomy.market;

import com.siirio.jemeconomy.shipping.ShippingBoxBlockEntity;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MarketRuntime {
    private static final Map<ServerLevel, Set<BlockPos>> LOADED = Collections.synchronizedMap(new WeakHashMap<>());
    private MarketRuntime() { }
    public static void register(ServerLevel level, BlockPos pos) { LOADED.computeIfAbsent(level, ignored -> new java.util.LinkedHashSet<>()).add(pos.immutable()); }
    public static void unregister(ServerLevel level, BlockPos pos) { Set<BlockPos> set = LOADED.get(level); if (set != null) set.remove(pos); }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0 || !event.getServer().isDedicatedServer()) return;
        long cycle = MarketSavedData.get(event.getServer()).cycle(event.getServer());
        synchronized (LOADED) {
            LOADED.forEach((level, positions) -> positions.removeIf(pos -> {
                if (!level.isLoaded(pos)) return true;
                if (level.getBlockEntity(pos) instanceof ShippingBoxBlockEntity box) { box.processScheduled(cycle); return false; }
                return true;
            }));
        }
    }
}
