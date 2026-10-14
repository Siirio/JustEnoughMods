package com.siirio.jemeconomy.market;

import com.siirio.jemeconomy.shipping.ShippingBoxBlockEntity;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MarketRuntime {
    private static final String BUYER_TAG = "jem_market_buyer";
    public static boolean isBuyer(net.minecraft.world.entity.Entity entity) { return entity.getTags().contains(BUYER_TAG); }
    private static final Map<ServerLevel, Set<BlockPos>> LOADED = Collections.synchronizedMap(new WeakHashMap<>());
    private MarketRuntime() { }
    public static void register(ServerLevel level, BlockPos pos) { LOADED.computeIfAbsent(level, ignored -> new java.util.LinkedHashSet<>()).add(pos.immutable()); }
    public static void unregister(ServerLevel level, BlockPos pos) { Set<BlockPos> set = LOADED.get(level); if (set != null) set.remove(pos); }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0 || !event.getServer().isDedicatedServer()) return;
        long cycle = MarketSavedData.get(event.getServer()).cycle(event.getServer());
        if (event.getServer().getTickCount() % 1200 == 0) reconcileBuyer(event.getServer().overworld());
        synchronized (LOADED) {
            LOADED.forEach((level, positions) -> positions.removeIf(pos -> {
                if (!level.isLoaded(pos)) return true;
                if (level.getBlockEntity(pos) instanceof ShippingBoxBlockEntity box) { box.processScheduled(cycle); return false; }
                return true;
            }));
        }
    }

    public static int bulletin(ServerPlayer player) {
        if (!com.siirio.jemidentity.api.DedicatedSmp.active(player)) return 0;
        // A transient native merchant supplies remote read-only offers; no NPC lookup/chunk load.
        Villager preview = net.minecraft.world.entity.EntityType.VILLAGER.create(player.serverLevel());
        if (preview == null) return 0;
        preview.addTag(BUYER_TAG); preview.addTag("jem_market_preview");
        preview.setCustomName(Component.translatable("jem.economy.buyer.preview_title"));
        com.siirio.jemeconomy.merchant.BuyerTradeOffers.refresh(preview, player);
        preview.setTradingPlayer(player);
        preview.openTradingScreen(player, preview.getDisplayName(), 1);
        return 1;
    }

    public static boolean isPreview(net.minecraft.world.entity.Entity entity) { return entity.getTags().contains("jem_market_preview"); }

    private static void reconcileBuyer(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos(); if (!level.hasChunkAt(spawn)) return;
        AABB area = new AABB(spawn).inflate(32.0D);
        var buyers = level.getEntitiesOfClass(Villager.class, area, villager -> villager.getTags().contains(BUYER_TAG));
        Villager buyer = buyers.isEmpty() ? null : buyers.get(0);
        for (int i = 1; i < buyers.size(); i++) buyers.get(i).discard();
        if (buyer == null) {
            buyer = net.minecraft.world.entity.EntityType.VILLAGER.create(level); if (buyer == null) return;
            buyer.moveTo(spawn.getX() + .5D, spawn.getY(), spawn.getZ() + .5D, 0.0F, 0.0F);
            if (!level.noCollision(buyer)) { buyer.discard(); return; }
            buyer.addTag(BUYER_TAG); buyer.setPersistenceRequired(); buyer.setInvulnerable(true); buyer.setNoAi(true);
            level.addFreshEntity(buyer);
        }
        var demand = MarketSavedData.get(level.getServer()).demand(level.getServer());
        buyer.setCustomName(Component.translatable("jem.economy.buyer.name", Component.translatable("jem.economy.demand." + demand.primary()), Component.translatable("jem.economy.demand." + demand.secondary())));
        buyer.setCustomNameVisible(true);
    }
}
