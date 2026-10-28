package com.justenoughmods.achievementguide.integration.magiclast;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
/** Pure observational furnace output receipt, independent from other producer families. */
public final class MagicLastResourceProof {
    private static final String KEY = "JemQuestArcaneOutputV4";
    public record Before(ItemStack output, CompoundTag proof) {}
    public record Take(BlockEntity owner, ItemStack output, CompoundTag proof, int held, long generation) {}
    private MagicLastResourceProof() {}
    public static Before before(BlockEntity owner) { return new Before(((Container)owner).getItem(2).copy(), owner.getPersistentData().getCompound(KEY).copy()); }
    public static void cooked(BlockEntity owner, Before before) { ItemStack output = ((Container)owner).getItem(2); int added = output.getCount() - before.output.getCount(); if (added <= 0 || !before.output.isEmpty() && !ItemStack.isSameItemSameTags(output, before.output)) return; int old = valid(before.proof, before.output) ? before.proof.getInt("known") : 0; CompoundTag proof = new CompoundTag(); proof.putInt("known", old + added); proof.put("output", output.save(new CompoundTag())); owner.getPersistentData().put(KEY, proof); owner.setChanged(); }
    private static boolean valid(CompoundTag proof, ItemStack output) { return proof.getInt("known") > 0 && proof.getInt("known") <= output.getCount() && output.save(new CompoundTag()).equals(proof.getCompound("output")); }
    public static List<Take> capture(AbstractContainerMenu menu, ServerPlayer actor) { if (!MagicLastProof.real(actor) || menu.slots.size() > 256) return List.of(); List<Take> rows = new ArrayList<>(); for (var slot : menu.slots) if (slot.getContainerSlot() == 2 && slot.container instanceof BlockEntity owner) { CompoundTag proof = owner.getPersistentData().getCompound(KEY); if (valid(proof, slot.getItem()) && rows.size() < MagicLastProof.LIMIT) rows.add(new Take(owner, slot.getItem().copy(), proof.copy(), MagicLastProof.held(actor, slot.getItem()), owner.getPersistentData().getLong(KEY + "Generation"))); } return rows; }
    public static void collected(AbstractContainerMenu menu, ServerPlayer actor, List<Take> rows) { if (!MagicLastProof.real(actor) || actor.containerMenu != menu) return; for (Take row : rows) { ItemStack after = ((Container)row.owner).getItem(2); int removed = row.output.getCount() - after.getCount(); if (row.owner.getPersistentData().getLong(KEY + "Generation") != row.generation || removed <= 0 || !after.isEmpty() && !ItemStack.isSameItemSameTags(row.output, after) || MagicLastProof.held(actor, row.output) - row.held < removed) continue; int certified = Math.max(0, removed - (row.output.getCount() - row.proof.getInt("known"))), remaining = row.proof.getInt("known") - certified; if (remaining <= 0 || after.isEmpty()) row.owner.getPersistentData().remove(KEY); else { CompoundTag proof = row.proof.copy(); proof.putInt("known", remaining); proof.put("output", after.save(new CompoundTag())); row.owner.getPersistentData().put(KEY, proof); } row.owner.setChanged(); if (certified > 0) MagicLastProof.emit(actor, "v3_magic_arcane_resources"); } }
    public static void inserted(BlockEntity owner, ItemStack item) { if (!item.isEmpty()) owner.getPersistentData().putLong(KEY + "Generation", owner.getPersistentData().getLong(KEY + "Generation") + 1); invalidated(owner); }
    public static void invalidated(BlockEntity owner) { if (owner.getPersistentData().contains(KEY)) { owner.getPersistentData().remove(KEY); owner.setChanged(); } }
}
