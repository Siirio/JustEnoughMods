package com.siirio.jemcompat.gate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.UUID;

public final class CampaignEyeService {
    private CampaignEyeService() {
    }

    public static boolean isCampaignEye(ItemStack stack) {
        return CampaignBoss.byEye(BuiltInRegistries.ITEM.getKey(stack.getItem())).isPresent();
    }

    public static void deliver(ServerLevel level, Vec3 position, CampaignBoss boss, Collection<UUID> participants) {
        Item eye = BuiltInRegistries.ITEM.get(boss.eye());
        GateAdvancements.recipients(level, position, participants).stream()
                .filter(player -> !has(player, boss))
                .forEach(player -> give(player, new ItemStack(eye)));
    }

    public static int recover(ServerPlayer player) {
        CampaignSavedData data = CampaignSavedData.get(player.server);
        int recovered = 0;
        for (CampaignBoss boss : CampaignBoss.values()) {
            if (!data.defeated(boss.entity()) || has(player, boss)) {
                continue;
            }
            give(player, new ItemStack(BuiltInRegistries.ITEM.get(boss.eye())));
            recovered++;
        }
        return recovered;
    }

    public static void protect(ItemEntity entity) {
        entity.setInvulnerable(true);
        entity.setUnlimitedLifetime();
        entity.clearFire();
    }

    private static boolean has(ServerPlayer player, CampaignBoss boss) {
        return player.getInventory().contains(new ItemStack(BuiltInRegistries.ITEM.get(boss.eye())));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            ItemEntity dropped = player.drop(stack, false);
            if (dropped != null) {
                protect(dropped);
            }
        }
        player.containerMenu.broadcastChanges();
    }
}
