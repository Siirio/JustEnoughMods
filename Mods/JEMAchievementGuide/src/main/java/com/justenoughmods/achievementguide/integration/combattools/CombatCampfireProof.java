package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

public final class CombatCampfireProof {
    private static final String KEY = "JemQuestCampfireInputV4";
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { final ArrayDeque<Place> places = new ArrayDeque<>(); final ArrayDeque<Cook> cooks = new ArrayDeque<>(); int placeOverflow, cookOverflow; }
    private record Place(CampfireBlockEntity owner, Entity actor, ItemStack input, int count, List<ItemStack> before) {}
    private static final class Product {
        final int slot; final UUID actor; final ItemStack output; final List<ItemEntity> drops = new ArrayList<>();
        Product(int slot, UUID actor, ItemStack output) { this.slot = slot; this.actor = actor; this.output = output.copy(); }
    }
    private record Cook(CampfireBlockEntity owner, Map<ItemStack, Product> outputs) {}
    private CombatCampfireProof() {}
    public static void clear() { STATE.remove(); }
    public static void placeBegin(CampfireBlockEntity owner, Entity actor, ItemStack input) {
        State state = STATE.get(); if (state.placeOverflow > 0 || state.places.size() >= CombatToolProof.LIMIT) { state.placeOverflow++; return; }
        List<ItemStack> before = new ArrayList<>(); for (ItemStack item : owner.getItems()) before.add(item.copy());
        state.places.push(new Place(owner, actor, input, input.getCount(), before));
    }
    public static void placeEnd(boolean accepted) {
        State state = STATE.get(); if (state.placeOverflow > 0) { state.placeOverflow--; return; }
        Place run = state.places.isEmpty() ? null : state.places.pop(); clean(state);
        if (run == null || !accepted || !CombatToolProof.real(run.actor) || run.count - run.input.getCount() != 1) return;
        for (int slot = 0; slot < Math.min(4, run.before.size()); slot++) if (run.before.get(slot).isEmpty() && CombatToolProof.item(run.owner.getItems().get(slot)).equals("rottencreatures:frozen_rotten_flesh")) {
            CompoundTag proof = new CompoundTag(); proof.putUUID("actor", run.actor.getUUID()); proof.put("input", run.owner.getItems().get(slot).save(new CompoundTag()));
            run.owner.getPersistentData().put(KEY + slot, proof); run.owner.setChanged();
        }
    }
    public static void cookBegin(CampfireBlockEntity owner) {
        State state = STATE.get(); if (state.cookOverflow > 0 || state.cooks.size() >= CombatToolProof.LIMIT) { state.cookOverflow++; return; }
        state.cooks.push(new Cook(owner, new IdentityHashMap<>()));
    }
    public static void assembled(Container input, Level level, CampfireCookingRecipe recipe, ItemStack output) {
        State state = STATE.get(); if (state.cookOverflow > 0 || state.cooks.isEmpty() || level.isClientSide || !recipe.getId().toString().equals("rottencreatures:rotten_flesh_from_campfire_cooking")) return;
        Cook run = state.cooks.peek(); if (run.outputs.size() >= 4 || !CombatToolProof.item(input.getItem(0)).equals("rottencreatures:frozen_rotten_flesh") || output.isEmpty()) return;
        for (int slot = 0; slot < Math.min(4, run.owner.getItems().size()); slot++) if (run.owner.getItems().get(slot) == input.getItem(0)) {
            CompoundTag proof = run.owner.getPersistentData().getCompound(KEY + slot);
            if (proof.hasUUID("actor") && input.getItem(0).save(new CompoundTag()).equals(proof.getCompound("input"))) run.outputs.put(output, new Product(slot, proof.getUUID("actor"), output));
            break;
        }
    }
    public static void spawned(ItemStack source, Entity entity, boolean accepted) {
        State state = STATE.get(); if (!accepted || !(entity instanceof ItemEntity drop) || state.cookOverflow > 0 || state.cooks.isEmpty()) return;
        Product output = state.cooks.peek().outputs.get(source);
        if (output != null && output.drops.size() < 4 && ItemStack.isSameItemSameTags(drop.getItem(), output.output)) output.drops.add(drop);
    }
    public static void cookEnd() {
        State state = STATE.get(); if (state.cookOverflow > 0) { state.cookOverflow--; return; }
        Cook run = state.cooks.isEmpty() ? null : state.cooks.pop(); clean(state); if (run == null) return;
        for (Product output : run.outputs.values()) {
            if (!run.owner.getItems().get(output.slot).isEmpty()) continue;
            int actual = output.drops.stream().filter(drop -> !drop.isRemoved()).mapToInt(drop -> drop.getItem().getCount()).sum();
            if (actual != output.output.getCount()) continue;
            CombatDropProof.enter(new CombatDropProof.Context(List.of(output.actor), "frozen_brewing_processed"));
            try { for (ItemEntity drop : output.drops) CombatDropProof.spawned(drop, !drop.isRemoved()); } finally { CombatDropProof.leave(); }
            run.owner.getPersistentData().remove(KEY + output.slot); run.owner.setChanged();
        }
    }
    private static void clean(State state) { if (state.places.isEmpty() && state.cooks.isEmpty()) STATE.remove(); }
}
