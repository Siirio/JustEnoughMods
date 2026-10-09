package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class CookingStations {
    private static final String OWNER = "jem:cooking_operator";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) return;
        BlockEntity station = player.level().getBlockEntity(event.getPos());
        if (station == null || !(station instanceof net.minecraft.world.level.block.entity.CampfireBlockEntity)
                && !(station instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)
                && !station.getClass().getName().startsWith("net.satisfy.")) return;
        station.getPersistentData().putUUID(OWNER, player.getUUID());
        station.setChanged();
    }

    public static Map<Item, Integer> snapshot(Object instance) {
        if (!(instance instanceof BlockEntity station) || !(station.getLevel() instanceof ServerLevel level)
                || EventScheduler.active(level.getServer(), "COOKING_SHOW") == null
                || !(instance instanceof Container inventory)) return null;
        var amounts = new HashMap<Item, Integer>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (!stack.isEmpty()) amounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return amounts;
    }

    public static void completed(Object instance, Map<Item, Integer> before) {
        if (before == null || !(instance instanceof BlockEntity station) || !(station.getLevel() instanceof ServerLevel level)
                || !station.getPersistentData().hasUUID(OWNER)) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(station.getPersistentData().getUUID(OWNER));
        if (player == null) return;
        var after = snapshot(instance);
        if (after == null) return;
        after.forEach((item, total) -> {
            int made = total - before.getOrDefault(item, 0);
            if (made > 0) CookingShow.progress(player, new ItemStack(item, made));
        });
    }

    public static void produced(Object instance, ItemStack stack) {
        if (!(instance instanceof BlockEntity station) || !(station.getLevel() instanceof ServerLevel level)
                || !station.getPersistentData().hasUUID(OWNER)) return;
        var player = level.getServer().getPlayerList().getPlayer(station.getPersistentData().getUUID(OWNER));
        if (player != null) CookingShow.progress(player, stack);
    }

    private CookingStations() {}
}
