package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.*;

import com.siirio.jemworldbosstiers.encounter.PendingRewardContainer;
import java.util.UUID;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
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

    public static UUID attempt(EventState row) {
        if (!row.hasAttemptId()) row.attemptId( UUID.randomUUID());
        return row.attemptId();
    }

    public static ItemStack mark(ItemStack stack, UUID event) {
        if(!stack.isEmpty()) stack.getOrCreateTag().putUUID(EVENT,event);
        return stack;
    }

    public static boolean belongsTo(ItemStack stack, UUID event) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().hasUUID(EVENT) && stack.getTag().getUUID(EVENT).equals(event);
    }

    public static void discard(MinecraftServer server, EventState row) {
        UUID event=attempt(row);
        var invalidated=new java.util.ArrayList<>(row.invalidatedLoot());
        if (!invalidated.contains(event)) invalidated.add(event);
        row.invalidatedLoot(invalidated);
        PendingRewardContainer.discardBySource(server,row.members().keySet(),event.toString());
        for(var player:server.getPlayerList().getPlayers()) {
            scrub(player.getInventory(),event);
            scrub(player.getEnderChestInventory(),event);
            player.inventoryMenu.broadcastChanges();
        }
        scrubRecovery(row,event);
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
        if(hasPendingLoot(container)) return;
        boolean changed=false;
        for(int slot=0;slot<container.getContainerSize();slot++) if(belongsTo(container.getItem(slot),event)) {
            container.setItem(slot,ItemStack.EMPTY);
            changed=true;
        }
        if(changed) container.setChanged();
    }

    private static void scrubInvalid(Container container, MinecraftServer server) {
        if(!hasInvalidatedLoot(server) || hasPendingLoot(container)) return;
        boolean changed=false;
        for(int slot=0;slot<container.getContainerSize();slot++) if(invalidated(server,container.getItem(slot))) {
            container.setItem(slot,ItemStack.EMPTY);
            changed=true;
        }
        if(changed) container.setChanged();
    }

    private static boolean hasInvalidatedLoot(MinecraftServer server) {
        for(var row:com.siirio.jemserver.smp.SmpData.get(server).events().snapshots())
            if(!row.invalidatedLoot().isEmpty()) return true;
        return false;
    }

    private static boolean hasPendingLoot(Container container) {
        // A pending native table has no materialized event loot to invalidate.
        // Reading its slots would generate ordinary loot during chunk/entity loading.
        if(container instanceof RandomizableContainerBlockEntity blockEntity)
            return blockEntity.saveWithoutMetadata().contains(RandomizableContainerBlockEntity.LOOT_TABLE_TAG,Tag.TAG_STRING);
        return container instanceof ContainerEntity entity && entity.getLootTable()!=null;
    }

    private static boolean invalidated(MinecraftServer server, ItemStack stack) {
        if(stack.isEmpty() || !stack.hasTag() || !stack.getTag().hasUUID(EVENT)) return false;
        UUID attempt=stack.getTag().getUUID(EVENT);
        for(var row:com.siirio.jemserver.smp.SmpData.get(server).events().snapshots())
            if(row.invalidatedLoot().contains(attempt)) return true;
        return false;
    }

    public static boolean activeAttempt(EventState row, Entity entity) {
        var data=entity.getPersistentData();
        return row.combatStarted() && row.hasAttemptId() && data.hasUUID(ATTEMPT_ID)
                && row.attemptId().equals(data.getUUID(ATTEMPT_ID));
    }

    private static void scrubRecovery(EventState row, UUID event) {
        for (var member : row.members().values()) {
            if (member.hasSafeInventory()) {
                ListTag items = member.safeInventory().compoundList();
                if (scrubList(items,event)) member.safeInventory(new NativeNbtPayload(items));
            }
            if (member.hasSafeDrops()) {
                ListTag items = member.safeDrops().compoundList();
                if (scrubList(items,event)) member.safeDrops(new NativeNbtPayload(items));
            }
        }
    }

    private static boolean scrubList(ListTag items, UUID event) {
        boolean changed=false;
        for(int index=items.size()-1;index>=0;index--) if(belongsTo(ItemStack.of(items.getCompound(index)),event)) {
            items.remove(index);
            changed=true;
        }
        return changed;
    }

    private BloodMoonLoot() {}
}
