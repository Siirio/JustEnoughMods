package com.siirio.jemeconomy.full.market;

import com.siirio.jemeconomy.shipping.ShippingBoxBlockEntity;
import com.siirio.jemeconomy.market.MarketSavedData;
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

    @SubscribeEvent
    public static void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("market")
                .requires(source -> com.siirio.jemeconomy.api.EconomyApi.isEnabled(source.getServer()))
                .executes(context -> {
                    com.siirio.jemeconomy.network.EconomyNetwork.openMarket(context.getSource().getPlayerOrException());
                    return 1;
                }));
    }
    public static void register(ServerLevel level, BlockPos pos) { LOADED.computeIfAbsent(level, ignored -> new java.util.LinkedHashSet<>()).add(pos.immutable()); }
    public static void unregister(ServerLevel level, BlockPos pos) { Set<BlockPos> set = LOADED.get(level); if (set != null) set.remove(pos); }

    @SubscribeEvent public static void started(net.minecraftforge.event.server.ServerStartedEvent event) {
        if (!com.siirio.jemeconomy.api.EconomyApi.isEnabled(event.getServer())) return;
        for (String category : java.util.List.of("blocks","food","plants","resources","drops","building64"))
            com.siirio.jemeconomy.policy.MarketSupply.listings(event.getServer(),category);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0 || !com.siirio.jemeconomy.api.EconomyApi.isEnabled(event.getServer())) return;
        synchronized (LOADED) {
            LOADED.forEach((level, positions) -> positions.removeIf(pos -> {
                if (!level.isLoaded(pos)) return true;
                if (level.getBlockEntity(pos) instanceof ShippingBoxBlockEntity box) { box.refresh(); return false; }
                return true;
            }));
        }
    }

    /** Retire only the synthetic trading role; native villager inventory and identity survive. */
    @SubscribeEvent public static void retireBuyer(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !com.siirio.jemeconomy.api.EconomyApi.isEnabled(event.getLevel().getServer())
                || !(event.getEntity() instanceof Villager villager) || !isBuyer(villager)) return;
        villager.removeTag(BUYER_TAG); villager.removeTag("jem_market_preview");
        villager.setInvulnerable(false); villager.setNoAi(false); villager.setCustomName(null); villager.setCustomNameVisible(false);
        ((com.siirio.jemeconomy.mixin.AbstractVillagerOffersAccessor) villager).jem$setOffers(new net.minecraft.world.item.trading.MerchantOffers());
    }
}
