package com.siirio.jemworldbosstiers.revival;

import com.siirio.jemworldbosstiers.mixin.EntityStorageAccessor;
import com.siirio.jemworldbosstiers.mixin.PersistentEntityStorageAccessor;
import com.siirio.jemworldbosstiers.mixin.ServerLevelEntityManagerAccessor;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;

/** A bounded native entity-chunk write acknowledges creation or retirement before ownership commits. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "jem_world_boss_tiers")
public final class NativeBossStorage {
    private static final ThreadLocal<Write> CURRENT = new ThreadLocal<>();
    private static final String CREATION = "JEMBossCreationPending";
    private static final String NATIVE_AI = "JEMBossCreationNativeNoAi";

    static final class Write {
        final IOWorker worker;
        final ChunkPos chunk;
        final UUID boss;
        final boolean present;
        CompletableFuture<Void> future;

        Write(IOWorker worker, ChunkPos chunk, UUID boss, boolean present) {
            this.worker = worker;
            this.chunk = chunk;
            this.boss = boss;
            this.present = present;
        }

        void observed(CompoundTag tag, CompletableFuture<Void> actualWrite) {
            if (future != null) throw new IllegalStateException("Multiple native boss writes in one commit");
            boolean included = tag != null && tag.getList("Entities", Tag.TAG_COMPOUND).stream()
                    .map(value -> (CompoundTag) value).anyMatch(value -> contains(value, boss));
            if (included != present) throw new IllegalStateException("Native boss serialization disagrees with ownership intent");
            future = actualWrite;
        }

        void await() {
            if (future == null) throw new IllegalStateException("Native entity write was not observed");
            future.join();
        }
    }

    private NativeBossStorage() { }

    @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void rejectRetiredIdentity(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof net.minecraft.world.entity.LivingEntity
                && com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer())
                .bossIdentityRetired(event.getEntity().getUUID())) event.setCanceled(true);
    }

    private static boolean contains(CompoundTag entity, UUID boss) {
        if (entity.hasUUID("UUID") && boss.equals(entity.getUUID("UUID"))) return true;
        return entity.getList("Passengers", Tag.TAG_COMPOUND).stream()
                .map(value -> (CompoundTag) value).anyMatch(value -> contains(value, boss));
    }

    public static void capture(IOWorker worker, ChunkPos chunk, CompoundTag tag, CompletableFuture<Void> future) {
        Write write = CURRENT.get();
        if (write != null && write.worker == worker && write.chunk.equals(chunk)) write.observed(tag, future);
    }

    public static boolean commit(ServerLevel level, ChunkPos chunk, UUID boss, boolean present) {
        if (!level.getServer().isSameThread() || !level.hasChunkAt(chunk.getWorldPosition())
                || !level.areEntitiesLoaded(chunk.toLong()) || CURRENT.get() != null) return false;
        var manager = ((ServerLevelEntityManagerAccessor) level).jemWorldBossTiers$entityManager();
        var access = (PersistentEntityStorageAccessor) manager;
        if (!(access.jemWorldBossTiers$permanentStorage() instanceof EntityStorage storage)) return false;
        IOWorker worker = ((EntityStorageAccessor) storage).jemWorldBossTiers$worker();
        Write write = new Write(worker, chunk, boss, present);
        CURRENT.set(write);
        try {
            if (!access.jemWorldBossTiers$storeChunkSections(chunk.toLong(), ignored -> { })) return false;
            if (write.future == null && !present) {
                // Native serialization proved this loaded chunk empty, but its
                // emptyChunks optimization also suppresses retries of failed writes.
                // Retry that exact native empty payload through its own IOWorker.
                worker.store(chunk, null);
            }
            write.await();
            worker.synchronize(true).join();
            return true;
        } catch (RuntimeException failure) {
            com.mojang.logging.LogUtils.getLogger().error("Native boss {} persistence remains unacknowledged", boss, failure);
            return false;
        } finally {
            CURRENT.remove();
        }
    }

    public static boolean commit(Entity boss) {
        return boss.level() instanceof ServerLevel level && commit(level, boss.chunkPosition(), boss.getUUID(), !boss.isRemoved());
    }

    public static boolean prepareCreation(ServerLevel level, ArenaRecord arena, net.minecraft.world.entity.LivingEntity boss) {
        if (com.siirio.jemworldbosstiers.api.WorldTierApi.profile(boss)
                .filter(com.siirio.jemworldbosstiers.balance.BossProfile::ownsExclusiveArena).isEmpty()) return false;
        var data = com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer());
        if (data.liveBoss(arena.id()).filter(id -> !id.equals(boss.getUUID())).isPresent()) return false;
        var flags = boss.getPersistentData();
        if (!flags.getBoolean(CREATION) && boss instanceof net.minecraft.world.entity.Mob mob) {
            flags.putBoolean(NATIVE_AI, mob.isNoAi());
            mob.setNoAi(true);
        }
        flags.putBoolean(CREATION, true);
        CompoundTag nativeEntity = boss.saveWithoutId(new CompoundTag());
        nativeEntity.putString("id", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString());
        data.beginBossCreation(arena.id(), nativeEntity);
        return data.recordLiveBoss(arena.id(), boss) && data.flush(level.getServer());
    }

    public static boolean finishCreation(ServerLevel level, ArenaRecord arena, net.minecraft.world.entity.LivingEntity boss) {
        var data = com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer());
        CompoundTag intent = data.pendingBossCreations().get(arena.id());
        if (intent == null) {
            if (!commit(boss)) return false;
            releaseCreationHold(boss);
            return true;
        }
        if (!intent.getUUID("UUID").equals(boss.getUUID()) || !commit(boss)) return false;
        data.clearBossCreation(arena.id(), boss.getUUID());
        if (!data.flush(level.getServer())) {
            data.beginBossCreation(arena.id(), intent);
            return false;
        }
        releaseCreationHold(boss);
        return true;
    }

    public static boolean pending(Entity entity) { return entity.getPersistentData().getBoolean(CREATION); }

    /** Profile migration releases only the obsolete creation hold, never support membership. */
    public static void releaseNonExclusiveCreation(net.minecraft.world.entity.LivingEntity boss) {
        if (com.siirio.jemworldbosstiers.api.WorldTierApi.profile(boss)
                .filter(profile -> !profile.ownsExclusiveArena()).isPresent()) releaseCreationHold(boss);
    }

    private static void releaseCreationHold(net.minecraft.world.entity.LivingEntity boss) {
        var flags = boss.getPersistentData();
        if (!flags.getBoolean(CREATION)) return;
        boolean nativeNoAi = flags.getBoolean(NATIVE_AI);
        flags.remove(CREATION);
        flags.remove(NATIVE_AI);
        if (flags.contains("JEMHostedLobby")) flags.getCompound("JEMHostedLobby").putBoolean("NativeNoAi", nativeNoAi);
        else if (boss instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(nativeNoAi);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof net.minecraft.world.entity.LivingEntity boss)
                || !pending(boss)) return;
        var data = com.siirio.jemworldbosstiers.progression.WorldTierData.get(level.getServer());
        boolean unresolved = data.pendingBossCreations().values().stream()
                .anyMatch(intent -> intent.getUUID("UUID").equals(boss.getUUID()));
        if (!unresolved) releaseCreationHold(boss);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void retryCreations(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        var server = event.getServer();
        var data = com.siirio.jemworldbosstiers.progression.WorldTierData.get(server);
        var entries = data.pendingBossCreations().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey()).toList();
        if (entries.isEmpty()) return;
        var pending = entries.get(Math.floorMod(server.getTickCount() / 20, entries.size()));
        ArenaRecord arena = data.arena(pending.getKey()).orElse(null);
        if (arena == null) return;
        var dimension = net.minecraft.resources.ResourceLocation.tryParse(arena.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, dimension));
        if (level == null || !ArenaService.entitiesLoaded(level, arena)) return;
        UUID expected = pending.getValue().getUUID("UUID");
        if (data.bossIdentityRetired(expected)) {
            data.clearBossCreation(arena.id(), expected);
            data.clearLiveBoss(arena.id(), expected);
            data.putArena(arena.release(expected));
            data.flush(server);
            return;
        }
        if (data.liveBoss(arena.id()).filter(id -> !id.equals(expected)).isPresent()) return;
        Entity existing = level.getEntity(expected);
        if (existing == null) {
            // This is replay of a durable never-released creation, not an inference that an unknown boss died.
            Entity replay = net.minecraft.world.entity.EntityType.loadEntityRecursive(pending.getValue().copy(), level, entity -> entity);
            if (!(replay instanceof net.minecraft.world.entity.LivingEntity boss) || !level.noCollision(boss)
                    || !level.areEntitiesLoaded(boss.chunkPosition().toLong()) || !data.recordLiveBoss(arena.id(), boss)
                    || !data.flush(server) || !level.addFreshEntity(boss)) return;
            existing = boss;
        }
        if (existing instanceof net.minecraft.world.entity.LivingEntity boss) {
            data.putArena(arena.recover(expected, level.getGameTime()));
            finishCreation(level, arena, boss);
        }
    }
}
