package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.CapturedMob;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.mobcatcher.MobCatcherStorage;

/** Bounded persisted quest-stage references to native captured records, never a second mob inventory. */
public final class BackpackCaptureProof {
    public static final String KEY = "JemQuestMobCaptureV4";
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private static final class Scope {
        final ServerPlayer actor;
        final LivingEntity target;
        IBackpackWrapper wrapper;
        UUID releaseId;
        CapturedMob record;
        boolean spawned;
        Scope(ServerPlayer actor, LivingEntity target, IBackpackWrapper wrapper) {
            this.actor = actor; this.target = target; this.wrapper = wrapper;
        }
    }
    private BackpackCaptureProof() {}
    private static boolean eligible(ServerPlayer actor) {
        return actor != null && !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild;
    }
    private static Scope pop() {
        Scope scope = CURRENT.get();
        CURRENT.remove();
        return scope;
    }
    public static void clear() { CURRENT.remove(); }

    public static void beginCapture(ServerPlayer actor, IBackpackWrapper wrapper, LivingEntity target) {
        CURRENT.set(new Scope(actor, target, wrapper));
    }
    public static void stored(IBackpackWrapper wrapper, CapturedMob record) {
        Scope scope = CURRENT.get();
        if (scope != null && scope.target != null && scope.wrapper == wrapper) scope.record = record;
    }
    public static void finishCapture() {
        Scope scope = pop();
        if (scope == null || !eligible(scope.actor) || scope.target == null || !scope.target.isRemoved()
                || scope.record == null || scope.record.hostile()
                || !MobCatcherStorage.getCapturedMob(scope.wrapper, scope.record.id()).filter(scope.record::equals).isPresent()) return;
        CompoundTag proofs = scope.actor.getPersistentData().getCompound(KEY);
        if (proofs.getAllKeys().size() >= 64) proofs.getAllKeys().stream()
                .min(Comparator.comparingLong(key -> proofs.getCompound(key).getLong("time"))).ifPresent(proofs::remove);
        CompoundTag row = new CompoundTag();
        row.putString("hash", fingerprint(scope.record));
        row.putLong("time", scope.actor.serverLevel().getGameTime());
        proofs.put(scope.record.id().toString(), row);
        scope.actor.getPersistentData().put(KEY, proofs);
    }

    public static void beginRelease(ServerPlayer actor, UUID id) {
        Scope scope = new Scope(actor, null, null);
        scope.releaseId = id;
        CURRENT.set(scope);
    }
    /** Observes the wrapper returned by the one native linked-storage resolution, without resolving again. */
    public static void releaseWrapper(IBackpackWrapper wrapper) {
        Scope scope = CURRENT.get();
        if (scope != null && scope.target == null && scope.releaseId != null && eligible(scope.actor)
                && scope.actor.containerMenu instanceof BackpackContainer menu && menu.stillValid(scope.actor) && wrapper != null) {
            scope.wrapper = wrapper;
            CapturedMob record = MobCatcherStorage.getCapturedMob(wrapper, scope.releaseId).orElse(null);
            CompoundTag proofs = scope.actor.getPersistentData().getCompound(KEY);
            if (record != null && !record.hostile() && fingerprint(record).equals(proofs.getCompound(scope.releaseId.toString()).getString("hash"))) scope.record = record;
            else { proofs.remove(scope.releaseId.toString()); scope.actor.getPersistentData().put(KEY, proofs); }
        }
    }
    public static void spawned(Entity entity, boolean accepted) {
        Scope scope = CURRENT.get();
        if (scope != null && scope.target == null && scope.record != null && accepted && entity instanceof LivingEntity living
                && entity.level() == scope.actor.serverLevel() && living.isAlive()
                && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(scope.record.entityType())) scope.spawned = true;
    }
    public static void finishRelease() {
        Scope scope = pop();
        if (scope == null || scope.record == null || !scope.spawned
                || MobCatcherStorage.getCapturedMob(scope.wrapper, scope.record.id()).isPresent()) return;
        CompoundTag proofs = scope.actor.getPersistentData().getCompound(KEY);
        proofs.remove(scope.record.id().toString());
        scope.actor.getPersistentData().put(KEY, proofs);
        QuestActionTrigger.INSTANCE.emit(scope.actor, "v3_store_mob_capture");
    }
    private static String fingerprint(CapturedMob record) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                    (record.entityType() + "\n" + record.entityNbt()).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
