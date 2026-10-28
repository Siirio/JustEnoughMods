package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Certified portions on actual native drops, including partial pickup and native merges. */
public final class CombatDropProof {
    public static final String KEY = "JemQuestCombatDropV4";
    public record Context(List<UUID> actors, String action) {}
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { final ArrayDeque<Context> contexts = new ArrayDeque<>(); final ArrayDeque<Merge> merges = new ArrayDeque<>(); int overflow, mergeOverflow; }
    private record Merge(ItemEntity destination, ItemEntity source, ItemStack to, ItemStack from, CompoundTag toProof, CompoundTag fromProof) {}
    private CombatDropProof() {}
    public static void clear() { STATE.remove(); }
    public static Runnable wrap(Supplier<Context> certificate, Runnable original) {
        return () -> { enter(certificate.get()); try { original.run(); } finally { leave(); } };
    }
    public static void enter(Context context) {
        State state = STATE.get();
        if (state.overflow > 0 || state.contexts.size() >= CombatToolProof.LIMIT) { state.overflow++; return; }
        state.contexts.push(context == null ? new Context(List.of(), "") : new Context(List.copyOf(context.actors.subList(0, Math.min(CombatToolProof.LIMIT, context.actors.size()))), context.action));
    }
    public static void leave() {
        State state = STATE.get(); if (state.overflow > 0) state.overflow--; else if (!state.contexts.isEmpty()) state.contexts.pop(); clean(state);
    }
    public static void spawned(Entity entity, boolean accepted) {
        State state = STATE.get();
        if (!accepted || !(entity instanceof ItemEntity item) || item.getItem().isEmpty() || state.overflow > 0 || state.contexts.isEmpty()) return;
        Context context = state.contexts.peek();
        if (context.actors.isEmpty() || context.action.isEmpty() || item.isRemoved()) return;
        CompoundTag proof = new CompoundTag(); ListTag actors = new ListTag();
        for (UUID actor : context.actors) { CompoundTag row = new CompoundTag(); row.putUUID("actor", actor); actors.add(row); }
        proof.put("actors", actors); proof.putString("action", context.action); update(item, proof, item.getItem().getCount());
    }
    private static int known(CompoundTag proof, ItemStack item) {
        return !proof.isEmpty() && proof.contains("output") && ItemStack.isSameItemSameTags(ItemStack.of(proof.getCompound("output")), item)
                ? Math.max(0, Math.min(proof.getInt("known"), item.getCount())) : 0;
    }
    private static void update(ItemEntity item, CompoundTag old, int known) {
        if (known <= 0 || item.getItem().isEmpty()) { item.getPersistentData().remove(KEY); return; }
        CompoundTag proof = old.copy(); proof.putInt("known", Math.min(known, item.getItem().getCount()));
        proof.put("output", item.getItem().save(new CompoundTag())); item.getPersistentData().put(KEY, proof);
    }
    public static void picked(ItemEntity item, Player player, ItemStack before, int held, CompoundTag proof) {
        if (!(player instanceof ServerPlayer actor) || before.isEmpty()) return;
        int taken = before.getCount() - item.getItem().getCount();
        if (taken <= 0 || CombatToolProof.held(actor, actor.containerMenu, before) - held < taken) return;
        int known = known(proof, before), certified = Math.max(0, taken - (before.getCount() - known)); update(item, proof, known - certified);
        if (!CombatToolProof.real(actor) || certified <= 0) return;
        ListTag actors = proof.getList("actors", Tag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(CombatToolProof.LIMIT, actors.size()); index++) {
            CompoundTag row = actors.getCompound(index);
            if (row.hasUUID("actor") && row.getUUID("actor").equals(actor.getUUID())) { CombatToolProof.emit(actor, proof.getString("action")); break; }
        }
    }
    public static void mergeBegin(ItemEntity destination, ItemEntity source) {
        State state = STATE.get();
        if (state.mergeOverflow > 0 || state.merges.size() >= CombatToolProof.LIMIT) { state.mergeOverflow++; return; }
        state.merges.push(new Merge(destination, source, destination.getItem().copy(), source.getItem().copy(),
                destination.getPersistentData().getCompound(KEY).copy(), source.getPersistentData().getCompound(KEY).copy()));
    }
    public static void mergeEnd(ItemEntity destination, ItemEntity source) {
        State state = STATE.get(); if (state.mergeOverflow > 0) { state.mergeOverflow--; return; }
        Merge merge = state.merges.isEmpty() ? null : state.merges.pop(); clean(state);
        if (merge == null || merge.destination != destination || merge.source != source) return;
        int transferred = merge.from.getCount() - source.getItem().getCount();
        if (transferred <= 0 || destination.getItem().getCount() - merge.to.getCount() != transferred) return;
        int to = known(merge.toProof, merge.to), from = known(merge.fromProof, merge.from), certified = Math.max(0, transferred - (merge.from.getCount() - from));
        boolean conflict = to > 0 && certified > 0 && (!merge.toProof.getString("action").equals(merge.fromProof.getString("action"))
                || !merge.toProof.getList("actors", Tag.TAG_COMPOUND).equals(merge.fromProof.getList("actors", Tag.TAG_COMPOUND)));
        update(destination, to > 0 ? merge.toProof : merge.fromProof, conflict ? 0 : to + certified); update(source, merge.fromProof, from - certified);
    }
    private static void clean(State state) { if (state.contexts.isEmpty() && state.merges.isEmpty()) STATE.remove(); }
}
