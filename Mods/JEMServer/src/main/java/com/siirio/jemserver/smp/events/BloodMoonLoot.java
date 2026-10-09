package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpRecords;
import com.siirio.jemworldbosstiers.encounter.PendingRewardContainer;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class BloodMoonLoot {
    private static final String EVENT="jem:blood_moon_loot_event";
    public static final String ATTEMPT_ID="jem:blood_moon_attempt";

    public static UUID attempt(CompoundTag row) {
        if (!row.hasUUID("attemptId")) row.putUUID("attemptId", UUID.randomUUID());
        return row.getUUID("attemptId");
    }

    public static ItemStack mark(ItemStack stack, UUID event) {
        if(!stack.isEmpty()) stack.getOrCreateTag().putUUID(EVENT,event);
        return stack;
    }

    public static boolean belongsTo(ItemStack stack, UUID event) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().hasUUID(EVENT) && stack.getTag().getUUID(EVENT).equals(event);
    }

    public static void discard(MinecraftServer server, CompoundTag row) {
        UUID event=attempt(row);
        var invalidated=row.getList("invalidatedLoot",Tag.TAG_INT_ARRAY);
        if (invalidated.stream().map(NbtUtils::loadUUID).noneMatch(event::equals)) invalidated.add(NbtUtils.createUUID(event));
        row.put("invalidatedLoot",invalidated);
        PendingRewardContainer.discardBySource(server,SmpRecords.memberIds(row),event.toString());
        for(var player:server.getPlayerList().getPlayers()) {
            scrub(player.getInventory(),event);
            scrub(player.getEnderChestInventory(),event);
            player.inventoryMenu.broadcastChanges();
        }
        scrubRecovery(row.getCompound("members"),event);
        for(var level:server.getAllLevels()) {
            for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(EventRegions.minX(row),level.getMinBuildHeight(),EventRegions.minZ(row),EventRegions.maxX(row)+1,level.getMaxBuildHeight(),EventRegions.maxZ(row)+1)))
                if(belongsTo(item.getItem(),event)) item.discard();
            for(Entity entity:level.getEntities((Entity)null,new AABB(EventRegions.minX(row),level.getMinBuildHeight(),EventRegions.minZ(row),EventRegions.maxX(row)+1,level.getMaxBuildHeight(),EventRegions.maxZ(row)+1),candidate->candidate instanceof Container))
                scrub((Container)entity,event);
            for(int chunkX=EventRegions.minX(row)>>4;chunkX<=EventRegions.maxX(row)>>4;chunkX++)
                for(int chunkZ=EventRegions.minZ(row)>>4;chunkZ<=EventRegions.maxZ(row)>>4;chunkZ++) {
                    var chunk=level.getChunkSource().getChunkNow(chunkX,chunkZ);
                    if(chunk==null) continue;
                    for(var blockEntity:chunk.getBlockEntities().values()) if(blockEntity instanceof Container container) scrub(container,event);
                }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        scrubInvalid(player.getInventory(),player.server);
        scrubInvalid(player.getEnderChestInventory(),player.server);
        player.inventoryMenu.broadcastChanges();
    }

    @SubscribeEvent
    public static void entity(EntityJoinLevelEvent event) {
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if(event.getEntity() instanceof ItemEntity item && invalidated(level.getServer(),item.getItem())) item.discard();
        else if(event.getEntity() instanceof Container container) scrubInvalid(container,level.getServer());
    }

    @SubscribeEvent
    public static void chunk(ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || !(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) return;
        for(var blockEntity:chunk.getBlockEntities().values()) if(blockEntity instanceof Container container) scrubInvalid(container,level.getServer());
    }

    private static void scrub(Container container, UUID event) {
        boolean changed=false;
        for(int slot=0;slot<container.getContainerSize();slot++) if(belongsTo(container.getItem(slot),event)) {
            container.setItem(slot,ItemStack.EMPTY);
            changed=true;
        }
        if(changed) container.setChanged();
    }

    private static void scrubInvalid(Container container, MinecraftServer server) {
        boolean changed=false;
        for(int slot=0;slot<container.getContainerSize();slot++) if(invalidated(server,container.getItem(slot))) {
            container.setItem(slot,ItemStack.EMPTY);
            changed=true;
        }
        if(changed) container.setChanged();
    }

    private static boolean invalidated(MinecraftServer server, ItemStack stack) {
        if(stack.isEmpty() || !stack.hasTag() || !stack.getTag().hasUUID(EVENT)) return false;
        UUID attempt=stack.getTag().getUUID(EVENT);
        for(var row:com.siirio.jemserver.smp.SmpData.get(server).all("events"))
            for(Tag tag:row.getList("invalidatedLoot",Tag.TAG_INT_ARRAY))
                if(NbtUtils.loadUUID(tag).equals(attempt)) return true;
        return false;
    }

    public static boolean activeAttempt(CompoundTag row, Entity entity) {
        var data=entity.getPersistentData();
        return row.getBoolean("combatStarted") && row.hasUUID("attemptId") && data.hasUUID(ATTEMPT_ID)
                && row.getUUID("attemptId").equals(data.getUUID(ATTEMPT_ID));
    }

    private static void scrubRecovery(CompoundTag members, UUID event) {
        for(String id:members.getAllKeys()) {
            var member=members.getCompound(id);
            scrubList(member.getList("safeInventory",Tag.TAG_COMPOUND),event);
            scrubList(member.getList("safeDrops",Tag.TAG_COMPOUND),event);
        }
    }

    private static void scrubList(ListTag items, UUID event) {
        for(int index=items.size()-1;index>=0;index--) if(belongsTo(ItemStack.of(items.getCompound(index)),event)) items.remove(index);
    }

    private BloodMoonLoot() {}
}
