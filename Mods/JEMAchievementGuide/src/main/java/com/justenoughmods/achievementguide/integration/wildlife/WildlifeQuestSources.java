package com.justenoughmods.achievementguide.integration.wildlife;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Shared finite physical item budgets reuse the existing pure portion API without changing ordinary NBT. */
public final class WildlifeQuestSources {
    public static final UUID NATURAL = new UUID(0, 0);
    public static final Set<String> NATURAL_KINDS = Set.of("wildlife_tooth", "wildlife_tentacle", "wildlife_flying_fish", "wildlife_antler");
    public static final class Pickup {
        public final ServerPlayer actor; public final ItemStack source; public final List<ItemStack> inventory;
        public Pickup(ServerPlayer actor, ItemStack source) {
            this.actor = actor; this.source = source.copy(); inventory = new ArrayList<>();
            for (int i = 0; i < actor.getInventory().getContainerSize(); i++) inventory.add(actor.getInventory().getItem(i).copy());
        }
    }
    private WildlifeQuestSources() {}
    public static void nativeDrop(ItemEntity drop, String kind, UUID actor) {
        if (drop != null && drop.level() instanceof ServerLevel level && level.getEntity(drop.getUUID()) == drop) {
            NativeFoodSources.get(level).certify(level, actor == null ? NATURAL : actor, kind, drop.getItem(), drop.getItem().getCount());
        }
    }
    public static void picked(Pickup run, ItemEntity drop) {
        if (run == null || WildlifeQuestProof.real(run.actor) == null) return;
        int removed = run.source.getCount() - (drop.isRemoved() ? 0 : drop.getItem().getCount());
        if (removed <= 0 || removed > run.source.getCount()) return;
        var physical = NativeFoodSources.get(run.actor.serverLevel()); ItemStack spent = NativeFoodSources.spent(run.source, removed);
        int received = 0;
        for (int i = 0; i < run.inventory.size(); i++) {
            ItemStack before = run.inventory.get(i), after = run.actor.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(after, run.source)) received += after.getCount() - (ItemStack.isSameItemSameTags(before, after) ? before.getCount() : 0);
        }
        if (received != removed) return;
        WildlifeQuestWorm.picked(run, spent, received);
        for (String kind : List.of("wildlife_fur", "wildlife_scute")) {
            if (run.actor.getUUID().equals(physical.owner(spent, kind)) && physical.consume(run.actor.getUUID(), spent, kind, 1))
                WildlifeQuestProof.emit(run.actor, kind.equals("wildlife_fur") ? "v3_fauna_renewable_harvest" : "v3_fauna_renewable_harvest_scute");
        }
        for (String kind : NATURAL_KINDS) {
            if (!NATURAL.equals(physical.owner(spent, kind))) continue;
            int known = NativeFoodSources.claims(spent).stream().mapToInt(NativeFoodSources.Claim::count).sum();
            if (known < 1 || !physical.consume(NATURAL, spent, kind, known)) continue;
            var oldBatches = NativeFoodSources.claims(spent).stream().map(NativeFoodSources.Claim::batch).toList();
            int left = known;
            for (int i = 0; i < run.inventory.size() && left > 0; i++) {
                ItemStack before = run.inventory.get(i), after = run.actor.getInventory().getItem(i);
                if (!ItemStack.isSameItemSameTags(after, run.source)) continue;
                int amount = Math.min(left, after.getCount() - (ItemStack.isSameItemSameTags(before, after) ? before.getCount() : 0));
                if (amount <= 0) continue;
                NativeFoodSources.claims(after, NativeFoodSources.claims(after).stream().filter(claim -> !oldBatches.contains(claim.batch())).toList());
                if (physical.certify(run.actor.serverLevel(), run.actor.getUUID(), kind, after, amount) != null) left -= amount;
            }
            if (left == 0 && kind.equals("wildlife_antler")) WildlifeQuestProof.emit(run.actor, "v3_fauna_renewable_harvest_antler");
        }
    }
    public static boolean consume(ServerPlayer actor, List<ItemStack> removed, String kind, int count) {
        var data = NativeFoodSources.get(actor.serverLevel());
        List<ItemStack> known = new ArrayList<>(); int available = 0;
        for (ItemStack stack : removed) {
            if (actor.getUUID().equals(data.owner(stack, kind))) { known.add(stack); available += NativeFoodSources.claims(stack).stream().mapToInt(NativeFoodSources.Claim::count).sum(); }
        }
        if (available < count) return false;
        int left = count;
        for (ItemStack stack : known) {
            int amount = Math.min(left, NativeFoodSources.claims(stack).stream().mapToInt(NativeFoodSources.Claim::count).sum());
            if (amount > 0 && data.consume(actor.getUUID(), stack, kind, amount)) left -= amount;
            if (left == 0) return true;
        }
        return false;
    }
    public static void durability(ServerLevel level, ItemStack before, ItemStack after) {
        if (before.isEmpty() || after.isEmpty() || before.getItem() != after.getItem() || ItemStack.isSameItemSameTags(before, after)) return;
        String kind = WildlifeQuestCrafts.TOOLS.get(WildlifeQuestProof.item(before)); if (kind == null) return;
        var data = NativeFoodSources.get(level); UUID owner = data.owner(before, kind);
        var tools = WildlifeQuestTools.get(level); UUID origin = tools.origin(before);
        if (owner == null || origin == null || !data.consume(owner, before, kind, 1)) return;
        var previous = NativeFoodSources.claims(before).stream().map(NativeFoodSources.Claim::batch).toList();
        NativeFoodSources.claims(after, NativeFoodSources.claims(after).stream().filter(claim -> !previous.contains(claim.batch())).toList());
        tools.retired(before); UUID batch = data.certify(level, owner, kind, after, 1); tools.record(batch, origin, after);
    }
}
