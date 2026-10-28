package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Independent bounded per-slot native output receipts for this family, never overwrites food receipts. */
public final class CombatStationProof {
    private static final String ROOT = "JemQuestCombatStationV4";
    public record Before(ItemStack item, CompoundTag proof) {}
    public record Collection(BlockEntity owner, Container inventory, int slot, ItemStack item, CompoundTag proof, int held) {}
    private CombatStationProof() {}
    private static String key(int slot) { return "slot" + slot; }
    public static Before before(BlockEntity owner, int slot) { return new Before(((Container)owner).getItem(slot).copy(), owner.getPersistentData().getCompound(ROOT).getCompound(key(slot)).copy()); }
    public static boolean matches(CompoundTag proof, ItemStack actual) {
        return !proof.isEmpty() && proof.getInt("known") > 0 && proof.getInt("known") <= actual.getCount()
                && actual.save(new CompoundTag()).equals(proof.getCompound("output"));
    }
    public static void completed(BlockEntity owner, int slot, String action, Before before, boolean transformed) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || slot < 0 || slot > 4) return;
        ItemStack actual = ((Container)owner).getItem(slot); if (actual.isEmpty()) return;
        int known;
        if (transformed) known = actual.getCount();
        else {
            if (!before.item.isEmpty() && !ItemStack.isSameItemSameTags(before.item, actual)) return;
            int added = actual.getCount() - before.item.getCount(); if (added <= 0) return;
            known = added + (matches(before.proof, before.item) && before.proof.getString("action").equals(action) ? before.proof.getInt("known") : 0);
        }
        CompoundTag root = owner.getPersistentData().getCompound(ROOT), proof = new CompoundTag();
        long generation = root.getLong("generation" + slot) + 1; root.putLong("generation" + slot, generation);
        proof.putLong("generation", generation); proof.putString("action", action); proof.putInt("known", Math.min(known, actual.getCount())); proof.put("output", actual.save(new CompoundTag()));
        root.put(key(slot), proof); owner.getPersistentData().put(ROOT, root); owner.setChanged();
    }
    public static void replaced(BlockEntity owner, int slot) {
        CompoundTag root = owner.getPersistentData().getCompound(ROOT); if (root.contains(key(slot))) { root.remove(key(slot)); owner.setChanged(); }
    }
    public static void inserted(BlockEntity owner, int slot, ItemStack item) {
        CompoundTag root = owner.getPersistentData().getCompound(ROOT), proof = root.getCompound(key(slot));
        if (!item.isEmpty() && root.contains("generation" + slot)) {
            ItemStack prior = ItemStack.of(proof.getCompound("output"));
            if (prior.isEmpty() || !ItemStack.isSameItemSameTags(prior, item) || item.getCount() >= prior.getCount()) root.putLong("generation" + slot, root.getLong("generation" + slot) + 1);
        }
        replaced(owner, slot);
    }
    public static void clear(BlockEntity owner) { if (owner.getPersistentData().contains(ROOT)) { owner.getPersistentData().remove(ROOT); owner.setChanged(); } }
    public static List<Collection> capture(AbstractContainerMenu menu, ServerPlayer actor) {
        if (!CombatToolProof.real(actor) || menu.slots.size() > 256) return List.of();
        List<Collection> rows = new ArrayList<>();
        for (var slot : menu.slots) {
            int index = slot.getContainerSlot();
            if (!(slot.container instanceof BlockEntity owner) || index < 0 || index > 4 || !(owner instanceof Container inventory)) continue;
            CompoundTag proof = owner.getPersistentData().getCompound(ROOT).getCompound(key(index));
            if (matches(proof, slot.getItem())) rows.add(new Collection(owner, inventory, index, slot.getItem().copy(), proof.copy(), CombatToolProof.held(actor, menu, slot.getItem())));
            if (rows.size() >= CombatToolProof.LIMIT) break;
        }
        return rows;
    }
    public static void collected(AbstractContainerMenu menu, ServerPlayer actor, List<Collection> rows) {
        if (actor.containerMenu != menu || !CombatToolProof.real(actor)) return;
        for (Collection row : rows) {
            ItemStack after = row.inventory.getItem(row.slot); int taken = row.item.getCount() - after.getCount();
            CompoundTag root = row.owner.getPersistentData().getCompound(ROOT);
            if (taken <= 0 || !after.isEmpty() && !ItemStack.isSameItemSameTags(row.item, after)
                    || CombatToolProof.held(actor, menu, row.item) - row.held < taken || root.getLong("generation" + row.slot) != row.proof.getLong("generation")) continue;
            int certified = Math.max(0, taken - (row.item.getCount() - row.proof.getInt("known"))), retained = row.proof.getInt("known") - certified;
            if (retained <= 0 || after.isEmpty()) root.remove(key(row.slot));
            else { CompoundTag proof = row.proof.copy(); proof.putInt("known", retained); proof.put("output", after.save(new CompoundTag())); root.put(key(row.slot), proof); }
            row.owner.setChanged(); if (certified > 0) CombatToolProof.emit(actor, row.proof.getString("action"));
        }
    }
}
