package com.siirio.jemeconomy.physical;

import com.siirio.jemeconomy.ledger.EconomySavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Lazy bounded migration of inventories, visible containers and dropped bearer coins. */
public final class CoinMigration {
    private CoinMigration() {}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.server.isDedicatedServer() || player.tickCount % 10 != 0) return;
        var data = EconomySavedData.get(player.server);
        for (var slot : player.containerMenu.slots) {
            if (slot.getItem().getItem() instanceof PhysicalCoinItem) { data.normalizeCoin(slot.getItem()); slot.setChanged(); }
        }
        player.containerMenu.broadcastChanges();
    }
    @SubscribeEvent public static void dropped(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ItemEntity item)
                || !event.getLevel().getServer().isDedicatedServer() || !(item.getItem().getItem() instanceof PhysicalCoinItem)) return;
        var stack = item.getItem().copy();
        EconomySavedData.get(event.getLevel().getServer()).normalizeCoin(stack);
        item.setItem(stack);
    }
}
