package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemserver.smp.SmpData;
import com.siirio.jemworldbosstiers.encounter.HostedEncounters;
import com.siirio.jemworldbosstiers.encounter.PendingRewardContainer;
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

@Mod.EventBusSubscriber(modid = "jem_server")
public final class BloodMoonDeaths {
    private static final String RECORDED = "jem:blood_moon_death_recorded";
    private static final String HOSTED_INVENTORY = "jem:hosted_inventory";

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void death(LivingDeathEvent event) {
        var serverForMutation = event.getEntity().getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { deathInScope(event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> deathInScope(event));
    }

    private static void deathInScope(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getPersistentData().getBoolean(RECORDED)) return;
        var row = EventSession.forPlayer(player);
        if (row == null) {
            if (!HostedEncounters.keepsInventory(player)) return;
            var inventory = new ListTag();
            player.getInventory().save(inventory);
            player.getPersistentData().put(HOSTED_INVENTORY, inventory);
            player.getInventory().clearContent();
            return;
        }
        player.getPersistentData().putBoolean(RECORDED, true);
        var member = row.member(player.getUUID());
        int deaths = member.actualDeaths() + 1;
        member.actualDeaths( deaths);
        int maximumLives=EventRules.FAILURE_DEATHS.get();
        member.awaitingReturn( deaths >= maximumLives);
        var inventory = new ListTag();
        player.getInventory().save(inventory);
        member.safeInventory(new NativeNbtPayload(inventory));
        member.safeRespawn( true);
        if(deaths>=maximumLives) member.eliminated(true);

        if(deaths>=maximumLives && row.members().keySet().stream().noneMatch(id->new EventSession(row).accepted(id))) BloodMoon.fail(player.server,row);
        SmpData.get(player.server).flush(player.server);
        if (member.safeRespawn()) player.getInventory().clearContent();
    }

    public static boolean hasRecovery(EventState row) {
        if (!row.activity().equals("BLOOD_MOON")) return false;
        for (var member : row.members().values()) {
            if (member.safeRespawn() || member.hasSafeInventory() || member.hasSafeDrops()) return true;
        }
        return false;
    }

    public static boolean keeps(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        return serverPlayer.getPersistentData().contains(HOSTED_INVENTORY, Tag.TAG_LIST)
                || HostedEncounters.keepsInventory(serverPlayer) || recovery(serverPlayer) != null;
    }

    private static EventState recovery(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { return recoveryInScope(player); }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        return store.edit(() -> recoveryInScope(player));
    }

    private static EventState recoveryInScope(ServerPlayer player) {
        for (var row : SmpData.get(player.server).events().currentRows()) {
            if (!row.activity().equals("BLOOD_MOON")) continue;
            var member = row.member(player.getUUID());
            if (member == null) continue;
            if (member.safeRespawn() || member.hasSafeInventory() || member.hasSafeDrops()) return row;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void keepOtherDrops(LivingDropsEvent event) {
        var serverForMutation = event.getEntity().getServer();
        if (serverForMutation == null || serverForMutation.overworld() == null) { keepOtherDropsInScope(event); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> keepOtherDropsInScope(event));
    }

    private static void keepOtherDropsInScope(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getPersistentData().contains(HOSTED_INVENTORY, Tag.TAG_LIST)) {
            event.getDrops().clear();
            return;
        }
        var row = recovery(player);
        if (row == null || event.getDrops().isEmpty()) return;
        var member = row.member(player.getUUID());
        if (member.hasSafeInventory()) {
            event.getDrops().clear();
            return;
        }
        var items = (member.hasSafeDrops() ? member.safeDrops().compoundList() : new ListTag());
        event.getDrops().forEach(drop -> items.add(drop.getItem().save(new CompoundTag())));
        member.safeDrops(new NativeNbtPayload(items));

        SmpData.get(player.server).flush(player.server);
        event.getDrops().clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getOriginal().getPersistentData().contains(HOSTED_INVENTORY, Tag.TAG_LIST)) {
            player.getInventory().load(event.getOriginal().getPersistentData().getList(HOSTED_INVENTORY, Tag.TAG_COMPOUND));
            event.getOriginal().getPersistentData().remove(HOSTED_INVENTORY);
            player.getPersistentData().remove(HOSTED_INVENTORY);
        }
        restore(player);
    }

    private static void restore(ServerPlayer player) {
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { restoreInScope(player); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> restoreInScope(player));
    }

    private static void restoreInScope(ServerPlayer player) {
        var row = recovery(player);
        if (row == null) return;
        var member = row.member(player.getUUID());
        boolean inventorySaved = member.hasSafeInventory();
        if (inventorySaved) {
            player.getInventory().load(member.safeInventory().compoundList());
            member.clearSafeInventory();
        }
        var extras = new ArrayList<ItemStack>();
        if (!inventorySaved)
            for (var tag : (member.hasSafeDrops() ? member.safeDrops().compoundList() : new ListTag())) extras.add(ItemStack.of((CompoundTag) tag));
        if (!extras.isEmpty())
            PendingRewardContainer.enqueue(player.server, player.getUUID(), "blood_moon_recovery:" + row.id(), "jem.smp.blood_moon_recovery", extras);
        member.clearSafeDrops();

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
        var serverForMutation = player.server;
        if (serverForMutation == null || serverForMutation.overworld() == null) { finishRespawnInScope(player); return; }
        var store = com.siirio.jemserver.smp.SmpData.get(serverForMutation);
        store.edit(() -> finishRespawnInScope(player));
    }

    private static void finishRespawnInScope(ServerPlayer player) {
        player.getPersistentData().remove(RECORDED);
        var row = recovery(player);
        if (row == null) row = EventSession.forPlayer(player);
        if (row == null) return;
        restore(player);
        var member = row.member(player.getUUID());
        if (member.safeRespawn()) player.setHealth((float) (player.getMaxHealth() * EventRules.RESPAWN_HEALTH.get()));
        if(member.eliminated()) {
            if(!EventRegions.returnOrigin(player,member)) EventRegions.eject(player,row);
        } else if (EventRuntime.enabled(player.server) && row.state().equals("ACTIVE")) {
            var level=EventRegions.level(player.server,row);
            var pos=level==null?null:new EncounterContext(row).respawnPoint(level);
            if(level!=null&&pos!=null) player.teleportTo(level, pos.getX() + .5, pos.getY(), pos.getZ() + .5, player.getYRot(), player.getXRot());
        }
        member.clearSafeRespawn();

    }

    private BloodMoonDeaths() {}
}
