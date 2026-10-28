package com.justenoughmods.achievementguide.integration.wildlife;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.siirio.jemrewards.PendingRewardContainer;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
public final class WildlifeQuestWorm extends SavedData {
    public record Claim(ServerPlayer actor, ListTag bundles, List<ItemStack> inventory) {}
    private static final int LIMIT = 8192;
    private final Map<String, Set<UUID>> sources = new LinkedHashMap<>();
    private final Map<UUID, Set<UUID>> drops = new LinkedHashMap<>();
    public static WildlifeQuestWorm get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(WildlifeQuestWorm::load, WildlifeQuestWorm::new, "jem_wildlife_worm_v4"); }
    public void settled(String source, Set<UUID> participants) {
        if (!sources.containsKey(source) && sources.size() >= LIMIT) sources.remove(sources.keySet().iterator().next());
        sources.put(source, Set.copyOf(participants)); setDirty();
    }
    public static boolean material(ItemStack stack) { return Set.of("alexsmobs:void_worm_mandible", "alexsmobs:void_worm_eye").contains(WildlifeQuestProof.item(stack)); }
    public static void dropped(ItemEntity item, Set<UUID> participants) {
        if (!(item.level() instanceof net.minecraft.server.level.ServerLevel level) || participants.isEmpty() || !material(item.getItem())) return;
        var store = get(level.getServer());
        if (store.drops.size() >= LIMIT) {
            UUID revoked = store.drops.keySet().iterator().next();
            store.drops.remove(revoked); NativeFoodSources.get(level).activate(revoked, false); store.setDirty();
        }
        var batch = NativeFoodSources.get(level).certify(level, WildlifeQuestSources.NATURAL, "wildlife_worm_loot", item.getItem(), item.getItem().getCount());
        if (batch != null) { store.drops.put(batch, Set.copyOf(participants)); store.setDirty(); }
    }
    public static void picked(WildlifeQuestSources.Pickup run, ItemStack spent, int received) {
        var physical = NativeFoodSources.get(run.actor.serverLevel()); var store = get(run.actor.server);
        if (!WildlifeQuestSources.NATURAL.equals(physical.owner(spent, "wildlife_worm_loot"))) return;
        var claims = NativeFoodSources.claims(spent); int count = claims.stream().mapToInt(NativeFoodSources.Claim::count).sum();
        if (count < 1 || count > received || claims.stream().anyMatch(c -> !store.drops.getOrDefault(c.batch(), Set.of()).contains(run.actor.getUUID())) || !physical.consume(WildlifeQuestSources.NATURAL, spent, "wildlife_worm_loot", count)) return;
        int left = count; var batches = claims.stream().map(NativeFoodSources.Claim::batch).toList();
        for (int i = 0; i < run.inventory.size() && left > 0; i++) {
            ItemStack before = run.inventory.get(i), after = run.actor.getInventory().getItem(i); if (!ItemStack.isSameItemSameTags(spent, after)) continue;
            int gain = after.getCount() - (ItemStack.isSameItemSameTags(before, after) ? before.getCount() : 0); int amount = Math.min(left, gain); if (amount <= 0) continue;
            NativeFoodSources.claims(after, NativeFoodSources.claims(after).stream().filter(c -> !batches.contains(c.batch())).toList());
            if (physical.certify(run.actor.serverLevel(), run.actor.getUUID(), "wildlife_worm_material", after, amount) != null) left -= amount;
        }
    }
    public static Claim beforeClaim(ServerPlayer actor) {
        if (WildlifeQuestProof.real(actor) == null) return null;
        return new Claim(actor, PendingRewardContainer.bundles(actor, "").copy(), actor.getInventory().items.stream().map(ItemStack::copy).toList());
    }
    private static int count(List<ItemStack> list, ItemStack variant) { return list.stream().filter(s -> ItemStack.isSameItemSameTags(s, variant)).mapToInt(ItemStack::getCount).sum(); }
    private static int bundleCount(ListTag rows, String source, ItemStack variant) {
        int count = 0; for (int i = 0; i < rows.size(); i++) { var row = rows.getCompound(i); if (!row.getString("sourceId").equals(source)) continue;
            var items = row.getList("items", 10); for (int j = 0; j < items.size(); j++) { var stack = ItemStack.of(items.getCompound(j)); if (ItemStack.isSameItemSameTags(stack, variant)) count += stack.getCount(); }
        } return count;
    }
    public static void claimed(Claim run, boolean accepted) {
        if (run == null || !accepted) return; var store = get(run.actor.server); var after = PendingRewardContainer.bundles(run.actor, ""); var physical = NativeFoodSources.get(run.actor.serverLevel());
        Set<String> handled = new HashSet<>();
        for (int i = 0; i < run.bundles.size(); i++) {
            var row = run.bundles.getCompound(i); String source = row.getString("sourceId"); if (!store.sources.getOrDefault(source, Set.of()).contains(run.actor.getUUID())) continue;
            var items = row.getList("items", 10);
            for (int j = 0; j < items.size(); j++) {
                var variant = ItemStack.of(items.getCompound(j)); if (!material(variant) || !handled.add(source + "/" + WildlifeQuestProof.item(variant))) continue;
                int transferred = bundleCount(run.bundles, source, variant) - bundleCount(after, source, variant);
                int gained = count(run.actor.getInventory().items, variant) - count(run.inventory, variant); int left = Math.min(transferred, gained); if (left <= 0) continue;
                for (int slot = 0; slot < run.inventory.size() && left > 0; slot++) {
                    var actual = run.actor.getInventory().items.get(slot); if (!ItemStack.isSameItemSameTags(actual, variant)) continue;
                    int gain = actual.getCount() - (ItemStack.isSameItemSameTags(actual, run.inventory.get(slot)) ? run.inventory.get(slot).getCount() : 0);
                    int amount = Math.min(left, Math.min(Math.max(0, gain), actual.getCount() - NativeFoodSources.claims(actual).stream().mapToInt(NativeFoodSources.Claim::count).sum()));
                    if (amount > 0 && physical.certify(run.actor.serverLevel(), run.actor.getUUID(), "wildlife_worm_material", actual, amount) != null) left -= amount;
                }
            }
        }
    }
    private static Set<UUID> actors(ListTag list) { Set<UUID> result = new HashSet<>(); for (Tag tag : list) result.add(NbtUtils.loadUUID(tag)); return Set.copyOf(result); }
    public static Set<UUID> participants(net.minecraft.world.entity.LivingEntity boss) { return actors(WildlifeQuestProof.data(boss).getList("worm_native_participants", 11)); }
    private static WildlifeQuestWorm load(CompoundTag tag) {
        var data = new WildlifeQuestWorm(); var rows = tag.getList("sources", 10); for (int i = 0; i < Math.min(rows.size(), LIMIT); i++) { var row = rows.getCompound(i); data.sources.put(row.getString("source"), actors(row.getList("actors", 11))); }
        rows = tag.getList("drops", 10); for (int i = 0; i < Math.min(rows.size(), LIMIT); i++) { var row = rows.getCompound(i); if (row.hasUUID("batch")) data.drops.put(row.getUUID("batch"), actors(row.getList("actors", 11))); } return data;
    }
    private static ListTag savedActors(Set<UUID> actors) { ListTag list = new ListTag(); actors.forEach(id -> list.add(NbtUtils.createUUID(id))); return list; }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag sourceRows = new ListTag(), dropRows = new ListTag(); sources.forEach((source, actors) -> { var row = new CompoundTag(); row.putString("source", source); row.put("actors", savedActors(actors)); sourceRows.add(row); });
        drops.forEach((batch, actors) -> { var row = new CompoundTag(); row.putUUID("batch", batch); row.put("actors", savedActors(actors)); dropRows.add(row); }); tag.put("sources", sourceRows); tag.put("drops", dropRows); return tag;
    }
}
