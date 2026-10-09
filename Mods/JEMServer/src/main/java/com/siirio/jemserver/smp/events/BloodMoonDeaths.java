package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemserver.smp.SmpRecords;
import com.siirio.jemworldbosstiers.encounter.PendingRewardContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class BloodMoonDeaths {
    private static final String RECORDED = "jem:blood_moon_death_recorded";
    private static final String ORDINARY = "jem:blood_moon_ordinary_death";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getPersistentData().getBoolean(RECORDED)) return;
        var row = EventSession.forPlayer(player);
        if (row == null) return;
        player.getPersistentData().putBoolean(RECORDED, true);
        var member = SmpRecords.members(row).getCompound(player.getStringUUID());
        int deaths = member.getInt("actualDeaths") + 1;
        member.putInt("actualDeaths", deaths);
        int maximumLives=EventRules.FAILURE_DEATHS.get();
        player.getPersistentData().putBoolean(ORDINARY, deaths > maximumLives);
        member.putBoolean("awaitingReturn", deaths >= maximumLives);
        if (deaths <= maximumLives) {
            var inventory = new ListTag();
            player.getInventory().save(inventory);
            member.put("safeInventory", inventory);
            member.putBoolean("safeRespawn", true);
        }
        if(deaths>=maximumLives) member.putBoolean("eliminated",true);
        SmpData.get(player.server).changed(row);
        if(deaths>=maximumLives && SmpRecords.memberIds(row).stream().noneMatch(id->new EventSession(row).accepted(id))) BloodMoon.fail(player.server,row);
        SmpData.get(player.server).flush(player.server);
        if (member.getBoolean("safeRespawn")) player.getInventory().clearContent();
    }

    public static boolean hasRecovery(CompoundTag row) {
        if (!row.getString("activity").equals("BLOOD_MOON")) return false;
        var members = SmpRecords.members(row);
        for (String id : members.getAllKeys()) {
            var member = members.getCompound(id);
            if (member.getBoolean("safeRespawn") || member.contains("safeInventory") || member.contains("safeDrops")) return true;
        }
        return false;
    }

    public static boolean ordinaryDeath(Player player) {
        return player instanceof ServerPlayer && player.getPersistentData().getBoolean(ORDINARY);
    }

    public static boolean protectsNextDeath(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        var row = EventSession.forPlayer(serverPlayer);
        return row != null && SmpRecords.members(row).getCompound(player.getStringUUID()).getInt("actualDeaths") < EventRules.FAILURE_DEATHS.get();
    }

    public static boolean keeps(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        return recovery(serverPlayer) != null;
    }

    private static CompoundTag recovery(ServerPlayer player) {
        for (var row : SmpData.get(player.server).all("events")) {
            if (!row.getString("activity").equals("BLOOD_MOON")) continue;
            var member = SmpRecords.members(row).getCompound(player.getStringUUID());
            if (member.getBoolean("safeRespawn") || member.contains("safeInventory") || member.contains("safeDrops")) return row;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void keepOtherDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var row = recovery(player);
        if (row == null || event.getDrops().isEmpty()) return;
        var member = SmpRecords.members(row).getCompound(player.getStringUUID());
        if (member.contains("safeInventory")) {
            event.getDrops().clear();
            return;
        }
        var items = member.getList("safeDrops", Tag.TAG_COMPOUND);
        event.getDrops().forEach(drop -> items.add(drop.getItem().save(new CompoundTag())));
        member.put("safeDrops", items);
        SmpData.get(player.server).changed(row);
        SmpData.get(player.server).flush(player.server);
        event.getDrops().clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) restore(player);
    }

    private static void restore(ServerPlayer player) {
        var row = recovery(player);
        if (row == null) return;
        var member = SmpRecords.members(row).getCompound(player.getStringUUID());
        boolean inventorySaved = member.contains("safeInventory");
        if (inventorySaved) {
            player.getInventory().load(member.getList("safeInventory", Tag.TAG_COMPOUND));
            member.remove("safeInventory");
        }
        var extras = new ArrayList<ItemStack>();
        if (!inventorySaved)
            for (var tag : member.getList("safeDrops", Tag.TAG_COMPOUND)) extras.add(ItemStack.of((CompoundTag) tag));
        if (!extras.isEmpty())
            PendingRewardContainer.enqueue(player.server, player.getUUID(), "blood_moon_recovery", "jem.smp.blood_moon_recovery", extras);
        member.remove("safeDrops");
        SmpData.get(player.server).changed(row);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) finishRespawn(player);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive()) finishRespawn(player);
    }

    private static void finishRespawn(ServerPlayer player) {
        player.getPersistentData().remove(RECORDED);
        player.getPersistentData().remove(ORDINARY);
        var row = recovery(player);
        if (row == null) row = EventSession.forPlayer(player);
        if (row == null) return;
        restore(player);
        var member = SmpRecords.members(row).getCompound(player.getStringUUID());
        if (member.getBoolean("safeRespawn")) player.setHealth((float) (player.getMaxHealth() * EventRules.RESPAWN_HEALTH.get()));
        if(member.getBoolean("eliminated")) {
            if(!EventRegions.returnOrigin(player,member)) EventRegions.eject(player,row);
        } else if (row.getString("state").equals("ACTIVE")) {
            var level=EventRegions.level(player.server,row);
            var pos=level==null?null:new EncounterContext(row).respawnPoint(level);
            if(level!=null&&pos!=null) player.teleportTo(level, pos.getX() + .5, pos.getY(), pos.getZ() + .5, player.getYRot(), player.getXRot());
        }
        member.remove("safeRespawn");
        SmpData.get(player.server).changed(row);
    }

    private BloodMoonDeaths() {}
}
