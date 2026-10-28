package com.justenoughmods.achievementguide.integration.food;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

/** Physical ingredient budgets and planted origins, never a player completion ledger. */
public final class NativeFoodSources extends SavedData {
    public static final String STACK_KEY = "JemNativeFoodSourcesV4";
    public record Claim(UUID batch, int count) {}
    public interface StackAccess {
        List<Claim> jem$foodSources();
        void jem$foodSources(List<Claim> claims);
    }
    public record Plant(UUID actor, String kind, String block) {}
    private record Batch(UUID actor, String kind, ItemStack variant, int remaining, boolean active) {}
    private final Map<UUID, Batch> batches = new LinkedHashMap<>();
    private final Map<String, Plant> plants = new LinkedHashMap<>();
    private static final int MAX_BATCHES = 16384;
    private static final int MAX_PLANTS = 16384;

    public static NativeFoodSources get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(NativeFoodSources::load, NativeFoodSources::new, "jem_native_food_sources_v4");
    }
    public static List<Claim> claims(ItemStack stack) {
        return stack.isEmpty() ? List.of() : ((StackAccess)(Object)stack).jem$foodSources();
    }
    public static void claims(ItemStack stack, List<Claim> claims) {
        if (!stack.isEmpty()) ((StackAccess)(Object)stack).jem$foodSources(List.copyOf(claims.subList(0, Math.min(8, claims.size()))));
    }
    public UUID certify(ServerLevel level, UUID actor, String kind, ItemStack output, int count) { return certify(actor, kind, output, count, true); }
    public UUID provisional(UUID actor, String kind, ItemStack output, int count) { return certify(actor, kind, output, count, false); }
    private UUID certify(UUID actor, String kind, ItemStack output, int count, boolean active) {
        if (actor == null || count < 1 || output.isEmpty() || count > output.getCount()) return null;
        List<Claim> old = new ArrayList<>(claims(output));
        old.removeIf(claim -> !batches.containsKey(claim.batch));
        if (old.size() >= 8 || old.stream().mapToInt(Claim::count).sum() + count > output.getCount()) return null;
        // Eviction revokes this exact UUID everywhere, including surviving physical copies.
        // Uncollected or destroyed sources must never permanently prevent new certification.
        if (batches.size() >= MAX_BATCHES) {
            UUID revoked = batches.keySet().iterator().next();
            batches.remove(revoked);
            old.removeIf(claim -> claim.batch.equals(revoked));
        }
        UUID id = UUID.randomUUID();
        ItemStack variant = output.copyWithCount(1);
        claims(variant, List.of());
        batches.put(id, new Batch(actor, kind, variant, count, active));
        old.add(new Claim(id, count)); claims(output, old); setDirty();
        return id;
    }
    public void activate(UUID id, boolean committed) {
        if (id == null) return;
        Batch batch = batches.get(id);
        if (batch == null) return;
        if (committed) batches.put(id, new Batch(batch.actor, batch.kind, batch.variant, batch.remaining, true));
        else batches.remove(id);
        setDirty();
    }
    public UUID owner(ItemStack snapshot, String kind) {
        for (Claim claim : claims(snapshot)) {
            Batch batch = batches.get(claim.batch);
            if (claim.count > 0 && batch != null && batch.active && batch.remaining > 0 && batch.kind.equals(kind)
                    && ItemStack.isSameItemSameTags(snapshot, batch.variant)) return batch.actor;
        }
        return null;
    }
    public boolean consume(UUID actor, ItemStack snapshot, String kind, int count) {
        if (actor == null || count < 1) return false;
        int available = 0;
        for (Claim claim : claims(snapshot)) {
            Batch batch = batches.get(claim.batch);
            if (batch != null && batch.active && actor.equals(batch.actor) && kind.equals(batch.kind) && ItemStack.isSameItemSameTags(snapshot, batch.variant)) {
                available += Math.min(claim.count, batch.remaining);
            }
        }
        if (available < count) return false;
        for (Claim claim : claims(snapshot)) {
            Batch batch = batches.get(claim.batch);
            if (batch == null || !batch.active || !actor.equals(batch.actor) || !kind.equals(batch.kind) || !ItemStack.isSameItemSameTags(snapshot, batch.variant)) continue;
            int used = Math.min(count, Math.min(claim.count, batch.remaining));
            int left = batch.remaining - used;
            if (left == 0) batches.remove(claim.batch); else batches.put(claim.batch, new Batch(batch.actor, batch.kind, batch.variant, left, true));
            count -= used;
            if (count == 0) break;
        }
        setDirty(); return true;
    }
    private static String key(ServerLevel level, BlockPos pos) { return level.dimension().location() + "/" + pos.asLong(); }
    public Plant plant(ServerLevel level, BlockPos pos) { return plants.get(key(level, pos)); }
    public void plant(ServerLevel level, BlockPos pos, Plant plant) {
        String key = key(level, pos);
        if (plant == null) plants.remove(key);
        else {
            if (!plants.containsKey(key) && plants.size() >= MAX_PLANTS) plants.remove(plants.keySet().iterator().next());
            plants.put(key, plant);
        }
        setDirty();
    }
    public static String block(BlockState state) { return String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())); }
    public static List<Claim> portion(List<Claim> claims, int oldCount, int moved) {
        int known = claims.stream().mapToInt(Claim::count).sum();
        int selected = Math.max(0, moved - Math.max(0, oldCount - known));
        List<Claim> result = new ArrayList<>();
        for (Claim claim : claims) {
            int take = Math.min(selected, claim.count);
            if (take > 0) result.add(new Claim(claim.batch, take));
            selected -= take;
        }
        return result;
    }
    public static List<Claim> remainder(List<Claim> claims, List<Claim> taken) {
        List<Claim> left = new ArrayList<>();
        for (Claim claim : claims) {
            int moved = taken.stream().filter(part -> part.batch.equals(claim.batch)).mapToInt(Claim::count).sum();
            if (claim.count > moved) left.add(new Claim(claim.batch, claim.count - moved));
        }
        return left;
    }
    public static ItemStack spent(ItemStack before, int count) {
        ItemStack portion = before.copyWithCount(count);
        claims(portion, portion(claims(before), before.getCount(), count));
        return portion;
    }
    public static void transfer(ItemStack target, List<Claim> source, int oldSourceCount, int moved) {
        if (target.isEmpty() || moved < 1) return;
        List<Claim> result = new ArrayList<>(claims(target));
        for (Claim claim : portion(source, oldSourceCount, moved)) {
            int at = -1;
            for (int i = 0; i < result.size(); i++) if (result.get(i).batch.equals(claim.batch)) { at = i; break; }
            if (at >= 0) result.set(at, new Claim(claim.batch, result.get(at).count + claim.count));
            else if (result.size() < 8) result.add(claim);
        }
        int known = result.stream().mapToInt(Claim::count).sum();
        claims(target, known <= target.getCount() ? result : List.of());
    }
    public static ListTag writeClaims(List<Claim> claims) {
        ListTag list = new ListTag();
        for (Claim claim : claims) { CompoundTag row = new CompoundTag(); row.putUUID("batch", claim.batch); row.putInt("count", claim.count); list.add(row); }
        return list;
    }
    public static List<Claim> readClaims(CompoundTag tag, int count) {
        ListTag list = tag.getList(STACK_KEY, 10); List<Claim> claims = new ArrayList<>(); int sum = 0;
        if (list.size() > 8) return List.of();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i); int known = row.getInt("count");
            if (!row.hasUUID("batch") || known < 1 || known > count || sum > count - known) return List.of();
            UUID batch = row.getUUID("batch");
            if (claims.stream().anyMatch(c -> c.batch.equals(batch))) return List.of();
            claims.add(new Claim(batch, known)); sum += known;
        }
        return claims;
    }
    private static NativeFoodSources load(CompoundTag tag) {
        NativeFoodSources data = new NativeFoodSources(); ListTag batches = tag.getList("batches", 10);
        for (int i = 0; i < Math.min(MAX_BATCHES, batches.size()); i++) {
            CompoundTag row = batches.getCompound(i); int count = row.getInt("remaining");
            ItemStack variant = ItemStack.of(row.getCompound("variant")); claims(variant, List.of());
            if (row.hasUUID("id") && row.hasUUID("actor") && count > 0 && count <= 4096 && !variant.isEmpty()) {
                data.batches.put(row.getUUID("id"), new Batch(row.getUUID("actor"), row.getString("kind"), variant, count, true));
            }
        }
        ListTag plants = tag.getList("plants", 10);
        for (int i = 0; i < Math.min(MAX_PLANTS, plants.size()); i++) {
            CompoundTag row = plants.getCompound(i);
            if (row.hasUUID("actor")) data.plants.put(row.getString("key"), new Plant(row.getUUID("actor"), row.getString("kind"), row.getString("block")));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag batchRows = new ListTag();
        batches.forEach((id, batch) -> {
            if (!batch.active) return;
            CompoundTag row = new CompoundTag(); row.putUUID("id", id); row.putUUID("actor", batch.actor);
            row.putString("kind", batch.kind); row.putInt("remaining", batch.remaining); row.put("variant", batch.variant.save(new CompoundTag())); batchRows.add(row);
        });
        ListTag plantRows = new ListTag();
        plants.forEach((key, plant) -> {
            CompoundTag row = new CompoundTag(); row.putString("key", key); row.putUUID("actor", plant.actor);
            row.putString("kind", plant.kind); row.putString("block", plant.block); plantRows.add(row);
        });
        tag.put("batches", batchRows); tag.put("plants", plantRows); return tag;
    }
}
