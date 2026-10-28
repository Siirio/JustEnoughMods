package com.justenoughmods.achievementguide.integration.wildlife;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.Item;
import java.util.ArrayDeque;
import net.minecraft.world.level.saveddata.SavedData;

/** Physical bucket lineage observes the native replacement UUID and never restores gameplay data. */
public final class WildlifeQuestBuckets extends SavedData {
    public record Receipt(UUID source, UUID actor, String type, CompoundTag state, CompoundTag observations) {}
    public record Capture(ServerPlayer actor, LivingEntity entity, ItemStack input, Item bucketItem, CompoundTag state, CompoundTag observations) {}
    private record Call(Player actor, InteractionHand hand, LivingEntity entity, Capture capture) {}
    private static final class Calls { final ArrayDeque<Call> rows = new ArrayDeque<>(); int overflow; }
    private final Map<UUID, Receipt> buckets = new LinkedHashMap<>();
    private static final int LIMIT = 4096;
    private static final ThreadLocal<Calls> CAPTURE = ThreadLocal.withInitial(Calls::new);
    public static WildlifeQuestBuckets get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(WildlifeQuestBuckets::load, WildlifeQuestBuckets::new, "jem_wildlife_buckets_v4");
    }
    public static CompoundTag state(Entity entity) {
        CompoundTag state = new CompoundTag(); entity.saveWithoutId(state);
        for (String key : java.util.List.of("UUID", "Pos", "Motion", "Rotation", "OnGround", "FallDistance")) state.remove(key);
        if (state.contains("ForgeData", 10)) {
            var data = state.getCompound("ForgeData"); data.remove(WildlifeQuestProof.KEY);
            if (data.isEmpty()) state.remove("ForgeData");
        }
        return state;
    }
    public static void before(Player player, InteractionHand hand, LivingEntity entity) {
        Calls calls = CAPTURE.get();
        if (calls.overflow > 0 || calls.rows.size() >= 16) { calls.overflow++; return; }
        Capture capture = null; var actor = WildlifeQuestProof.real(player); ItemStack input = player.getItemInHand(hand);
        if (actor != null && entity instanceof Bucketable bucketable && entity.isAlive() && entity.level() == actor.level()
                && input.is(Items.WATER_BUCKET) && input.getCount() == 1 && WildlifeQuestProof.type(entity).startsWith("alexsmobs:")) {
            var nativeState = state(entity); var observations = WildlifeQuestProof.data(entity).copy();
            if (WildlifeQuestProof.type(entity).equals("alexsmobs:catfish") && observations.hasUUID("catfish_swallowed_source")) {
                Entity swallowed = actor.serverLevel().getEntity(observations.getUUID("catfish_swallowed_source"));
                boolean valid = (swallowed == null || swallowed.isRemoved()) && nativeState.getBoolean("HasSwallowedEntity")
                        && nativeState.getString("ContainedEntityType").equals(observations.getString("catfish_swallowed_type"))
                        && nativeState.getCompound("ContainedData").equals(observations.getCompound("catfish_swallowed_data"));
                observations.putBoolean("catfish_valid_capture", valid);
            }
            capture = new Capture(actor, entity, input.copy(), bucketable.getBucketItemStack().getItem(), nativeState, observations);
        }
        calls.rows.push(new Call(player, hand, entity, capture));
    }
    public static void captured(Player player, InteractionHand hand, LivingEntity entity, boolean accepted) {
        Calls calls = CAPTURE.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Call call = calls.rows.poll(); if (calls.rows.isEmpty()) CAPTURE.remove();
        Capture run = call == null ? null : call.capture();
        if (run == null || call.actor() != player || call.hand() != hand || call.entity() != entity
                || WildlifeQuestProof.real(player) != run.actor() || run.entity() != entity || !accepted
                || entity.level() != player.level() || !entity.isRemoved() || entity.getRemovalReason() != Entity.RemovalReason.DISCARDED) return;
        ItemStack bucket = player.getItemInHand(hand);
        if (!(bucket.getItem() instanceof MobBucketItem) || bucket.getItem() != run.bucketItem() || bucket.getCount() != 1) return;
        WildlifeQuestFinders.captured(run.actor(), entity);
        var store = get(run.actor().serverLevel()); if (store.buckets.size() >= LIMIT) return;
        UUID batch = NativeFoodSources.get(run.actor().serverLevel()).certify(run.actor().serverLevel(), run.actor().getUUID(), "wildlife_bucket", bucket, 1);
        if (batch != null) {
            var expected = run.state().copy(); expected.putBoolean("FromBucket", true);
            store.buckets.put(batch, new Receipt(entity.getUUID(), run.actor().getUUID(), WildlifeQuestProof.type(entity), expected, run.observations().copy())); store.setDirty();
        }
    }
    public static WildlifeQuestRun beforeRelease(ServerPlayer actor, ItemStack stack) {
        if (WildlifeQuestProof.real(actor) == null || !(stack.getItem() instanceof MobBucketItem)
                || !actor.getUUID().equals(NativeFoodSources.get(actor.serverLevel()).owner(stack, "wildlife_bucket"))) return null;
        return WildlifeQuestRun.begin(actor, actor, stack);
    }
    public static void released(WildlifeQuestRun run, ItemStack after, boolean accepted) {
        WildlifeQuestRun.end(run);
        if (run == null || !accepted || !after.is(Items.BUCKET) || after.getCount() != 1 || run.input.getCount() != 1) return;
        var physical = NativeFoodSources.get(run.actor.serverLevel()); var store = get(run.actor.serverLevel());
        for (var claim : NativeFoodSources.claims(run.input)) {
            Receipt receipt = store.buckets.get(claim.batch()); if (receipt == null || !run.actor.getUUID().equals(receipt.actor())) continue;
            for (Entity entity : run.created) {
                if (!WildlifeQuestProof.type(entity).equals(receipt.type()) || run.actor.serverLevel().getEntity(entity.getUUID()) != entity || entity.isRemoved()) continue;
                boolean preserved = receipt.state().equals(state(entity));
                if (!physical.consume(run.actor.getUUID(), run.input, "wildlife_bucket", 1)) return;
                store.buckets.remove(claim.batch()); store.setDirty();
                if (!preserved) return;
                if (!receipt.observations().isEmpty()) entity.getPersistentData().put(WildlifeQuestProof.KEY, receipt.observations().copy());
                WildlifeQuestProof.mark(entity, "bucket_native_actor", run.actor.getUUID()); WildlifeQuestProof.data(entity).putUUID("bucket_native_source", receipt.source());
                if (receipt.type().equals("alexsmobs:blobfish")) WildlifeQuestProof.mark(entity, "captured_blobfish_owner", run.actor.getUUID());
                if (!receipt.type().equals("alexsmobs:catfish")) WildlifeQuestProof.emit(run.actor, "v3_fauna_creature_transport");
                return;
            }
        }
    }
    private static WildlifeQuestBuckets load(CompoundTag tag) {
        var data = new WildlifeQuestBuckets(); ListTag rows = tag.getList("buckets", 10);
        for (int i = 0; i < Math.min(LIMIT, rows.size()); i++) {
            var row = rows.getCompound(i);
            if (row.hasUUID("batch") && row.hasUUID("source") && row.hasUUID("actor")) data.buckets.put(row.getUUID("batch"), new Receipt(row.getUUID("source"), row.getUUID("actor"), row.getString("type"), row.getCompound("state").copy(), row.getCompound("observations").copy()));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag rows = new ListTag(); buckets.forEach((batch, receipt) -> {
            CompoundTag row = new CompoundTag(); row.putUUID("batch", batch); row.putUUID("source", receipt.source()); row.putUUID("actor", receipt.actor());
            row.putString("type", receipt.type()); row.put("state", receipt.state().copy()); row.put("observations", receipt.observations().copy()); rows.add(row);
        }); tag.put("buckets", rows); return tag;
    }
}
