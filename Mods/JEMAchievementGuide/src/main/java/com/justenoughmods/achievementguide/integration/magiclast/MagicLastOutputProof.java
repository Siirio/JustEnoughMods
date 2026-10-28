package com.justenoughmods.achievementguide.integration.magiclast;
import java.util.ArrayDeque;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
/** Certified native output portions, actual pickup and native merge conservation. */
public final class MagicLastOutputProof {
    public static final String KEY = "JemQuestMagicOutputV4";
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Merge> calls = new ArrayDeque<>(); int overflow; }
    private record Merge(ItemEntity to, ItemEntity from, ItemStack a, ItemStack b, CompoundTag x, CompoundTag y) {}
    private MagicLastOutputProof() {}
    public static void certify(ItemEntity actual, ServerPlayer actor, String action) { if (actual != null && !actual.isRemoved() && !actual.getItem().isEmpty() && MagicLastProof.real(actor)) { CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID()); proof.putString("action", action); update(actual, proof, actual.getItem().getCount()); } }
    private static int known(CompoundTag proof, ItemStack item) { return !proof.isEmpty() && ItemStack.isSameItemSameTags(ItemStack.of(proof.getCompound("output")), item) ? Math.max(0, Math.min(proof.getInt("known"), item.getCount())) : 0; }
    private static void update(ItemEntity item, CompoundTag source, int count) { if (count <= 0 || item.getItem().isEmpty()) { item.getPersistentData().remove(KEY); return; } CompoundTag proof = source.copy(); proof.putInt("known", Math.min(count, item.getItem().getCount())); proof.put("output", item.getItem().save(new CompoundTag())); item.getPersistentData().put(KEY, proof); }
    public static void picked(ItemEntity item, Player user, ItemStack before, int held, CompoundTag proof) {
        if (!(user instanceof ServerPlayer actor) || before.isEmpty()) return;
        int taken = before.getCount() - item.getItem().getCount(); if (taken <= 0 || MagicLastProof.held(actor, before) - held < taken) return;
        int known = known(proof, before), certified = Math.max(0, taken - (before.getCount() - known)); update(item, proof, known - certified);
        if (certified > 0 && proof.hasUUID("actor") && actor.getUUID().equals(proof.getUUID("actor"))) { if (proof.getString("action").equals("spell_cauldron_brew")) MagicLastProof.emit(actor, "spell_cauldron_native_brew"); MagicLastProof.emit(actor, proof.getString("action")); }
    }
    public static void mergeBegin(ItemEntity to, ItemEntity from) { Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.calls.size() >= MagicLastProof.LIMIT) calls.overflow++; else calls.calls.push(new Merge(to, from, to.getItem().copy(), from.getItem().copy(), to.getPersistentData().getCompound(KEY).copy(), from.getPersistentData().getCompound(KEY).copy())); }
    public static void mergeEnd(ItemEntity to, ItemEntity from) {
        Calls calls = CALLS.get(); if (calls.overflow > 0) { calls.overflow--; return; } Merge row = calls.calls.isEmpty() ? null : calls.calls.pop(); if (calls.calls.isEmpty()) CALLS.remove();
        if (row == null || row.to != to || row.from != from) return;
        int moved = row.b.getCount() - from.getItem().getCount(); if (moved <= 0 || to.getItem().getCount() - row.a.getCount() != moved) return;
        int a = known(row.x, row.a), b = known(row.y, row.b), transferred = Math.max(0, moved - (row.b.getCount() - b));
        boolean conflict = a > 0 && transferred > 0 && (!row.x.getString("action").equals(row.y.getString("action")) || !row.x.hasUUID("actor") || !row.y.hasUUID("actor") || !row.x.getUUID("actor").equals(row.y.getUUID("actor")));
        update(to, a > 0 ? row.x : row.y, conflict ? 0 : a + transferred); update(from, row.y, b - transferred);
    }
    public static void clear() { CALLS.remove(); }
}
